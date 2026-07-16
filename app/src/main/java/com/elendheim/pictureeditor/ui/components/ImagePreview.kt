package com.elendheim.pictureeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.elendheim.pictureeditor.engine.ImageEngine
import com.elendheim.pictureeditor.model.EditState
import com.elendheim.pictureeditor.model.NormPoint
import com.elendheim.pictureeditor.model.Vignette
import com.elendheim.pictureeditor.ui.LayerItem
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * The live canvas. It draws the edited base, then any added pictures on top.
 * Tap an added picture to select it (red outline), drag to move it and pinch to
 * resize or turn it. Tapping the empty canvas deselects and returns you to the
 * base picture. The colour edit rides on top as a filter so sliders stay smooth.
 */
@Composable
fun ImagePreview(
    source: Bitmap,
    state: EditState,
    layers: List<LayerItem>,
    selectedLayerId: String?,
    onSelectLayer: (String?) -> Unit,
    onUpdateLayer: (String, NormPoint?, Float?, Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    val geo = remember(source, state.transform) {
        ImageEngine.geometry(source, state.transform)
    }
    val imageBitmap = remember(geo) { geo.asImageBitmap() }
    val colorFilter = remember(state.adjust) {
        if (state.adjust.isNeutral) null
        else ColorFilter.colorMatrix(ColorMatrix(ImageEngine.buildColorMatrix(state.adjust)))
    }
    val ratio = geo.width.toFloat() / geo.height.toFloat()
    val accent = Color(0xFFD65A5A)
    val density = LocalDensity.current

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val areaRatio = maxWidth.value / maxHeight.value
        val w = if (areaRatio > ratio) maxHeight * ratio else maxWidth
        val h = if (areaRatio > ratio) maxHeight else maxWidth / ratio
        val wPx = with(density) { w.toPx() }
        val hPx = with(density) { h.toPx() }

        Box(
            Modifier
                .size(w, h)
                // A tap on empty canvas deselects any added picture.
                .pointerInput(layers.size) {
                    detectTapGestures { onSelectLayer(null) }
                }
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Preview of your edited photo",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.size(w, h)
            )
            if (!state.vignette.isNeutral) {
                VignetteOverlay(state.vignette, Modifier.size(w, h))
            }

            layers.forEach { layer ->
                val lwPx = layer.scale * wPx
                val lhPx = lwPx * layer.bitmap.height / layer.bitmap.width
                val lw = with(density) { lwPx.toDp() }
                val lh = with(density) { lhPx.toDp() }
                val selected = layer.id == selectedLayerId
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (layer.center.x * wPx - lwPx / 2f).roundToInt(),
                                (layer.center.y * hPx - lhPx / 2f).roundToInt()
                            )
                        }
                        .size(lw, lh)
                        // Gestures are read before the rotation so panning maps
                        // straight onto the base picture's axes.
                        .pointerInput(layer.id, selected) {
                            if (selected) {
                                detectTransformGestures { _, pan, zoom, rot ->
                                    val nc = NormPoint(
                                        (layer.center.x + pan.x / wPx).coerceIn(0f, 1f),
                                        (layer.center.y + pan.y / hPx).coerceIn(0f, 1f)
                                    )
                                    onUpdateLayer(
                                        layer.id,
                                        nc,
                                        (layer.scale * zoom).coerceIn(0.05f, 3f),
                                        layer.rotationDeg + rot
                                    )
                                }
                            } else {
                                detectTapGestures { onSelectLayer(layer.id) }
                            }
                        }
                        .graphicsLayer { rotationZ = layer.rotationDeg }
                        .then(
                            if (selected) Modifier.border(2.dp, accent) else Modifier
                        )
                ) {
                    Image(
                        bitmap = layer.bitmap.asImageBitmap(),
                        contentDescription = "Added picture",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/** Draws the vignette on screen with the same radial fade the exporter uses. */
@Composable
private fun VignetteOverlay(vignette: Vignette, modifier: Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f
        val edge = if (vignette.amount >= 0f) {
            Color.Black.copy(alpha = vignette.amount.coerceIn(0f, 1f))
        } else {
            Color.White.copy(alpha = (-vignette.amount).coerceIn(0f, 1f))
        }
        val start = (vignette.size.coerceIn(0f, 1f) * (1f - vignette.feather.coerceIn(0f, 1f)))
            .coerceIn(0f, 0.98f)
        val brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                start to Color.Transparent,
                1f to edge
            ),
            center = center,
            radius = radius,
            tileMode = TileMode.Clamp
        )
        drawRect(brush = brush)
    }
}
