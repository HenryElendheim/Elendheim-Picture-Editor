package com.elendheim.pictureeditor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.elendheim.pictureeditor.engine.ImageEngine
import com.elendheim.pictureeditor.model.NormRect
import com.elendheim.pictureeditor.model.Transform
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Full screen crop tool. The whole picture stays visible; a bright frame in the
 * chosen aspect sits on top. Drag the frame to choose what to keep, pinch to
 * size it (smaller frame = more zoom). Because the frame you see IS the crop,
 * the result always matches exactly what was on screen. It snaps to the grid.
 */
@Composable
fun CropScreen(
    source: Bitmap,
    transform: Transform,
    onApply: (NormRect) -> Unit,
    onCancel: () -> Unit
) {
    // Crop the rotated / flipped picture so the frame lines up with the base.
    val rotated = remember(source, transform.rotationDeg, transform.flipH, transform.flipV) {
        ImageEngine.rotateFlip(source, transform)
    }
    val iw = rotated.width.toFloat()
    val ih = rotated.height.toFloat()
    val imageBitmap = remember(rotated) { rotated.asImageBitmap() }
    val density = LocalDensity.current

    // On-screen aspect of the frame: the chosen ratio, or the picture's own.
    val ratio = transform.aspect.ratio ?: (iw / ih)
    // Turn a frame width fraction into its height fraction for this ratio.
    val hFactor = (iw / ih) / ratio

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0B0A))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val areaW = constraints.maxWidth.toFloat()
            val areaH = constraints.maxHeight.toFloat()

            // Fit the whole picture inside the area, fully visible.
            val fitScale = min(areaW / iw, areaH / ih)
            val dispW = iw * fitScale
            val dispH = ih * fitScale
            val imgLeft = (areaW - dispW) / 2f
            val imgTop = (areaH - dispH) / 2f

            // The biggest frame width that still fits (its height must be <= 1).
            val maxBw = min(1f, 1f / hFactor)

            val stateKey = "$areaW$areaH$ratio$iw$ih"
            // Start from the existing crop, or a centred frame of this ratio.
            val start = remember(stateKey) {
                val c = transform.crop
                if (c != null && !c.isFull) {
                    Triple((c.left + c.right) / 2f, (c.top + c.bottom) / 2f, c.width)
                } else {
                    val cc = ImageEngine.centeredCrop(iw.toInt(), ih.toInt(), ratio)
                    Triple((cc.left + cc.right) / 2f, (cc.top + cc.bottom) / 2f, cc.width)
                }
            }
            var cxN by remember(stateKey) { mutableFloatStateOf(start.first) }
            var cyN by remember(stateKey) { mutableFloatStateOf(start.second) }
            var bwN by remember(stateKey) { mutableFloatStateOf(start.third.coerceAtMost(maxBw)) }

            val bhN = bwN * hFactor
            val dispWdp = with(density) { dispW.toDp() }
            val dispHdp = with(density) { dispH.toDp() }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(stateKey) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Pinch resizes the frame. Out (zoom > 1) shrinks the
                            // frame, which zooms further into the picture.
                            val lo = min(0.1f, maxBw)
                            bwN = (bwN / zoom).coerceIn(lo, maxBw)
                            // Drag moves the frame with the finger.
                            cxN += pan.x / dispW
                            cyN += pan.y / dispH
                            cxN = snap(cxN)
                            cyN = snap(cyN)
                            val bh = bwN * hFactor
                            cxN = cxN.coerceIn(bwN / 2f, 1f - bwN / 2f)
                            cyN = cyN.coerceIn(bh / 2f, 1f - bh / 2f)
                        }
                    }
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Picture being cropped",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset(imgLeft.roundToInt(), imgTop.roundToInt()) }
                        .size(dispWdp, dispHdp)
                )
                Canvas(Modifier.fillMaxSize()) {
                    val bw = bwN * dispW
                    val bh = bhN * dispH
                    val bl = imgLeft + (cxN - bwN / 2f) * dispW
                    val bt = imgTop + (cyN - bhN / 2f) * dispH
                    val shade = Color(0xAA000000)
                    // Darken everything outside the frame.
                    drawRect(shade, Offset(0f, 0f), Size(areaW, bt))
                    drawRect(shade, Offset(0f, bt + bh), Size(areaW, areaH - bt - bh))
                    drawRect(shade, Offset(0f, bt), Size(bl, bh))
                    drawRect(shade, Offset(bl + bw, bt), Size(areaW - bl - bw, bh))
                    // The frame border.
                    drawRect(Color(0xFFD65A5A), Offset(bl, bt), Size(bw, bh), style = Stroke(width = 3f))
                    // Rule of thirds inside the frame.
                    val guide = Color(0x66FFFFFF)
                    drawLine(guide, Offset(bl + bw / 3f, bt), Offset(bl + bw / 3f, bt + bh))
                    drawLine(guide, Offset(bl + 2f * bw / 3f, bt), Offset(bl + 2f * bw / 3f, bt + bh))
                    drawLine(guide, Offset(bl, bt + bh / 3f), Offset(bl + bw, bt + bh / 3f))
                    drawLine(guide, Offset(bl, bt + 2f * bh / 3f), Offset(bl + bw, bt + 2f * bh / 3f))
                }
            }

            Text(
                text = "Crop - drag the frame, pinch to zoom",
                color = Color(0xFFEDE8E6),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            )

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel crop", tint = Color(0xFFEDE8E6))
                }
                IconButton(onClick = {
                    // The frame IS the crop, so this is an exact match of what
                    // was on screen.
                    val bh = bwN * hFactor
                    onApply(
                        NormRect(
                            (cxN - bwN / 2f).coerceIn(0f, 1f),
                            (cyN - bh / 2f).coerceIn(0f, 1f),
                            (cxN + bwN / 2f).coerceIn(0f, 1f),
                            (cyN + bh / 2f).coerceIn(0f, 1f)
                        )
                    )
                }) {
                    Icon(Icons.Filled.Check, contentDescription = "Apply crop", tint = Color(0xFFD65A5A))
                }
            }
        }
    }
}

// Snap a frame centre to the thirds and middle grid lines when it gets close.
private fun snap(v: Float): Float {
    val lines = floatArrayOf(1f / 3f, 0.5f, 2f / 3f)
    for (line in lines) if (abs(line - v) < 0.02f) return line
    return v
}
