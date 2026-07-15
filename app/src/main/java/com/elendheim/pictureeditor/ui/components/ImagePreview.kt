package com.elendheim.pictureeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elendheim.pictureeditor.engine.ImageEngine
import com.elendheim.pictureeditor.model.EditState
import com.elendheim.pictureeditor.model.Vignette
import kotlin.math.hypot

/**
 * The live canvas. Geometry (rotate / flip / crop) is baked into a small bitmap
 * only when the transform changes; the colour adjustments ride on top as a
 * colour filter so dragging a slider stays smooth. Same maths as the exporter,
 * so the preview is honest about the final result.
 */
@Composable
fun ImagePreview(
    source: Bitmap,
    state: EditState,
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

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Fit the image inside the available area while keeping its shape.
        val areaRatio = maxWidth.value / maxHeight.value
        val w = if (areaRatio > ratio) maxHeight * ratio else maxWidth
        val h = if (areaRatio > ratio) maxHeight else maxWidth / ratio

        Box(Modifier.size(w, h)) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Preview of your edited photo",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.size(w, h)
            )
            if (!state.vignette.isNeutral) {
                VignetteOverlay(
                    vignette = state.vignette,
                    modifier = Modifier
                        .size(w, h)
                        .semantics { contentDescription = "Vignette overlay" }
                )
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
