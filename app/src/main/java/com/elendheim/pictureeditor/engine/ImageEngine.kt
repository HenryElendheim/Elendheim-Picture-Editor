package com.elendheim.pictureeditor.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.elendheim.pictureeditor.model.AdjustParams
import com.elendheim.pictureeditor.model.EditState
import com.elendheim.pictureeditor.model.NormRect
import com.elendheim.pictureeditor.model.Transform
import com.elendheim.pictureeditor.model.Vignette
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * One added picture, ready to be drawn on the base. Positions are fractions of
 * the base image so the same placement works at preview and full resolution.
 */
data class PlacedLayer(
    val bitmap: Bitmap,
    val cx: Float,
    val cy: Float,
    val scale: Float,
    val rotationDeg: Float
)

/**
 * The rendering engine. It knows nothing about the UI; give it a source bitmap
 * and an EditState and it hands back a fresh, edited bitmap. The same colour
 * matrix it builds here is also used live in the preview, so what you see is
 * what you export, just at a different size.
 */
object ImageEngine {

    /**
     * Turn the adjustment sliders into a single 4x5 colour matrix. Building one
     * combined matrix means the whole colour edit is one cheap GPU friendly
     * operation, both in the live preview and at export time.
     */
    fun buildColorMatrix(a: AdjustParams): FloatArray {
        val cm = ColorMatrix()

        // Exposure -> multiply every channel. Range -1..1 maps to about 0.5x..2x.
        if (a.exposure != 0f) {
            val e = 2f.pow(a.exposure)
            cm.postConcat(scaleMatrix(e))
        }

        // Contrast -> pull values away from (or toward) the mid grey point.
        if (a.contrast != 0f) {
            val c = 1f + a.contrast
            val t = (1f - c) * 128f
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // Saturation -> ColorMatrix has this built in.
        if (a.saturation != 0f) {
            val sat = ColorMatrix()
            sat.setSaturation(1f + a.saturation)
            cm.postConcat(sat)
        }

        // Warmth and tint -> nudge the channels. Warmth trades red against blue,
        // tint pushes green toward magenta.
        if (a.warmth != 0f || a.tint != 0f) {
            val w = a.warmth * 40f
            val ti = a.tint * 40f
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, w,
                        0f, 1f, 0f, 0f, ti,
                        0f, 0f, 1f, 0f, -w,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // Brightness -> a flat offset added to every channel.
        if (a.brightness != 0f) {
            val b = a.brightness * 100f
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, b,
                        0f, 1f, 0f, 0f, b,
                        0f, 0f, 1f, 0f, b,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // Hue -> rotate the colours around the luminance axis.
        if (a.hue != 0f) {
            cm.postConcat(hueMatrix(a.hue))
        }

        return cm.array
    }

    private fun scaleMatrix(s: Float): ColorMatrix = ColorMatrix(
        floatArrayOf(
            s, 0f, 0f, 0f, 0f,
            0f, s, 0f, 0f, 0f,
            0f, 0f, s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Standard luminance preserving hue rotation (same idea as SVG hue-rotate).
    private fun hueMatrix(deg: Float): ColorMatrix {
        val rad = deg / 180f * Math.PI.toFloat()
        val c = cos(rad)
        val s = sin(rad)
        val lr = 0.213f
        val lg = 0.715f
        val lb = 0.072f
        return ColorMatrix(
            floatArrayOf(
                lr + c * (1 - lr) + s * (-lr), lg + c * (-lg) + s * (-lg), lb + c * (-lb) + s * (1 - lb), 0f, 0f,
                lr + c * (-lr) + s * 0.143f, lg + c * (1 - lg) + s * 0.140f, lb + c * (-lb) + s * (-0.283f), 0f, 0f,
                lr + c * (-lr) + s * (-(1 - lr)), lg + c * (-lg) + s * lg, lb + c * (1 - lb) + s * lb, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /**
     * Replay the whole recipe onto a copy of the source. This is used for
     * export (pass the full size bitmap) and can also make a preview bitmap.
     * The source is never modified; a brand new bitmap comes back.
     */
    fun render(source: Bitmap, state: EditState, layers: List<PlacedLayer> = emptyList()): Bitmap {
        val geo = geometry(source, state.transform)
        val out = Bitmap.createBitmap(geo.width, geo.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (!state.adjust.isNeutral) {
            paint.colorFilter = ColorMatrixColorFilter(buildColorMatrix(state.adjust))
        }
        canvas.drawBitmap(geo, 0f, 0f, paint)
        if (!state.vignette.isNeutral) {
            drawVignette(canvas, out.width, out.height, state.vignette)
        }
        // Added pictures sit on top of the finished base.
        if (layers.isNotEmpty()) {
            composite(canvas, out.width, out.height, layers)
        }
        // Recycle the intermediate if we made a new one.
        if (geo !== source) geo.recycle()
        return out
    }

    /**
     * The base geometry for the preview: rotate, flip and crop, but no colour.
     * The live preview colours are applied on top with a colour filter so
     * dragging a slider never has to rebuild this bitmap.
     */
    fun geometry(src: Bitmap, t: Transform): Bitmap {
        val rotated = rotateFlip(src, t)
        val crop = t.crop
        if (crop == null || crop.isFull) {
            return if (rotated === src) copyOf(src) else rotated
        }
        val cropped = applyCrop(rotated, crop)
        if (rotated !== src && rotated !== cropped) rotated.recycle()
        return cropped
    }

    /** Rotation and flips only, no crop. Used by the interactive crop tool. */
    fun rotateFlip(src: Bitmap, t: Transform): Bitmap {
        if (t.rotationDeg == 0f && !t.flipH && !t.flipV) return src
        val m = Matrix()
        if (t.flipH) m.postScale(-1f, 1f)
        if (t.flipV) m.postScale(1f, -1f)
        if (t.rotationDeg != 0f) m.postRotate(t.rotationDeg)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    // Cut the fractional rectangle out of the bitmap.
    private fun applyCrop(src: Bitmap, rect: NormRect): Bitmap {
        val x = (rect.left * src.width).toInt().coerceIn(0, src.width - 1)
        val y = (rect.top * src.height).toInt().coerceIn(0, src.height - 1)
        val w = (rect.width * src.width).toInt().coerceIn(1, src.width - x)
        val h = (rect.height * src.height).toInt().coerceIn(1, src.height - y)
        return Bitmap.createBitmap(src, x, y, w, h)
    }

    // Copy so callers can always safely recycle the intermediate result.
    private fun copyOf(src: Bitmap): Bitmap = src.copy(Bitmap.Config.ARGB_8888, false)

    /**
     * A centred crop rectangle of the given ratio for a fresh aspect pick, so
     * choosing "Square" immediately frames the middle of the photo.
     */
    fun centeredCrop(imgWidth: Int, imgHeight: Int, ratio: Float): NormRect {
        val imgRatio = imgWidth.toFloat() / imgHeight.toFloat()
        return if (imgRatio > ratio) {
            // Image is wider than the target -> keep full height, trim sides.
            val w = ratio / imgRatio
            val inset = (1f - w) / 2f
            NormRect(inset, 0f, 1f - inset, 1f)
        } else {
            // Image is taller -> keep full width, trim top and bottom.
            val h = imgRatio / ratio
            val inset = (1f - h) / 2f
            NormRect(0f, inset, 1f, 1f - inset)
        }
    }

    // Draw each added picture layer over the base, centred and scaled.
    private fun composite(canvas: Canvas, w: Int, h: Int, layers: List<PlacedLayer>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        for (layer in layers) {
            val targetW = layer.scale * w
            val targetH = targetW * layer.bitmap.height / layer.bitmap.width
            val m = Matrix()
            // Scale the layer bitmap to its on-screen size.
            m.postScale(targetW / layer.bitmap.width, targetH / layer.bitmap.height)
            // Rotate around the layer centre.
            m.postRotate(layer.rotationDeg, targetW / 2f, targetH / 2f)
            // Move so the layer centre lands on its normalised position.
            m.postTranslate(layer.cx * w - targetW / 2f, layer.cy * h - targetH / 2f)
            canvas.drawBitmap(layer.bitmap, m, paint)
        }
    }

    // Paint the darkening (or lightening) toward the edges with a radial fade.
    private fun drawVignette(canvas: Canvas, w: Int, h: Int, v: Vignette) {
        val cx = w / 2f
        val cy = h / 2f
        val maxR = hypot(w.toDouble(), h.toDouble()).toFloat() / 2f
        val edge: Int = if (v.amount >= 0f) {
            Color.argb((v.amount.coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0)
        } else {
            Color.argb((-v.amount.coerceIn(-1f, 0f) * 255).toInt(), 255, 255, 255)
        }
        // The clear area shrinks as size grows; feather softens the ramp.
        val start = (v.size.coerceIn(0f, 1f) * (1f - v.feather.coerceIn(0f, 1f)))
            .coerceIn(0f, 0.98f)
        val shader = RadialGradient(
            cx, cy, maxR,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, edge),
            floatArrayOf(0f, start, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = shader
        canvas.drawRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)
    }
}
