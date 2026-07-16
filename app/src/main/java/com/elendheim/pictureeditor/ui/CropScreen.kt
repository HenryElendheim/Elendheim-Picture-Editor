package com.elendheim.pictureeditor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.elendheim.pictureeditor.engine.ImageEngine
import com.elendheim.pictureeditor.model.NormRect
import com.elendheim.pictureeditor.model.Transform
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Full screen crop tool, in the spirit of Snapseed. The frame stays put in the
 * chosen aspect ratio while you drag the picture around and pinch to zoom, so
 * you decide exactly what sits inside the frame. Apply writes the framed region
 * back as the crop.
 */
@Composable
fun CropScreen(
    source: Bitmap,
    transform: Transform,
    onApply: (NormRect) -> Unit,
    onCancel: () -> Unit
) {
    // Work on the rotated / flipped picture so the crop lines up with the base.
    val rotated = remember(source, transform.rotationDeg, transform.flipH, transform.flipV) {
        ImageEngine.rotateFlip(source, transform)
    }
    val iw = rotated.width.toFloat()
    val ih = rotated.height.toFloat()
    val imageBitmap = remember(rotated) { rotated.asImageBitmap() }
    val density = LocalDensity.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0B0A))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val areaW = constraints.maxWidth.toFloat()
            val areaH = constraints.maxHeight.toFloat()

            // The frame ratio: the chosen aspect, or the picture's own shape.
            val frameRatio = transform.aspect.ratio ?: (iw / ih)
            val margin = 0.86f
            val fw: Float
            val fh: Float
            if ((areaW * margin) / (areaH * margin) > frameRatio) {
                fh = areaH * margin
                fw = fh * frameRatio
            } else {
                fw = areaW * margin
                fh = fw / frameRatio
            }

            // The smallest zoom that still covers the frame with the picture.
            val coverScale = max(fw / iw, fh / ih)
            val stateKey = "$areaW$areaH$frameRatio"
            var scale by remember(stateKey) { mutableFloatStateOf(coverScale) }
            var tx by remember(stateKey) { mutableFloatStateOf(0f) }
            var ty by remember(stateKey) { mutableFloatStateOf(0f) }

            val cx = areaW / 2f
            val cy = areaH / 2f

            // Keep the frame filled: the picture may never pull away from an edge.
            fun clamp() {
                val maxTx = max(0f, (iw * scale - fw) / 2f)
                val maxTy = max(0f, (ih * scale - fh) / 2f)
                tx = tx.coerceIn(-maxTx, maxTx)
                ty = ty.coerceIn(-maxTy, maxTy)
            }

            val imgW = with(density) { (iw * scale).toDp() }
            val imgH = with(density) { (ih * scale).toDp() }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(stateKey) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(coverScale, coverScale * 8f)
                            tx += pan.x
                            ty += pan.y
                            clamp()
                        }
                    }
            ) {
                // The picture, centred then nudged by the pan offset.
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Picture being cropped",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (cx + tx - iw * scale / 2f).roundToInt(),
                                (cy + ty - ih * scale / 2f).roundToInt()
                            )
                        }
                        .size(imgW, imgH)
                )
                // The dark mask and the bright frame with rule of thirds guides.
                androidx.compose.foundation.Canvas(Modifier.size(with(density) { areaW.toDp() }, with(density) { areaH.toDp() })) {
                    val fl = cx - fw / 2f
                    val ft = cy - fh / 2f
                    val shade = Color(0xAA000000)
                    // Four rectangles around the frame.
                    drawRect(shade, topLeft = Offset(0f, 0f), size = Size(areaW, ft))
                    drawRect(shade, topLeft = Offset(0f, ft + fh), size = Size(areaW, areaH - ft - fh))
                    drawRect(shade, topLeft = Offset(0f, ft), size = Size(fl, fh))
                    drawRect(shade, topLeft = Offset(fl + fw, ft), size = Size(areaW - fl - fw, fh))
                    // Frame border.
                    drawRect(
                        color = Color(0xFFD65A5A),
                        topLeft = Offset(fl, ft),
                        size = Size(fw, fh),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                    // Rule of thirds.
                    val guide = Color(0x55FFFFFF)
                    drawLine(guide, Offset(fl + fw / 3f, ft), Offset(fl + fw / 3f, ft + fh))
                    drawLine(guide, Offset(fl + 2f * fw / 3f, ft), Offset(fl + 2f * fw / 3f, ft + fh))
                    drawLine(guide, Offset(fl, ft + fh / 3f), Offset(fl + fw, ft + fh / 3f))
                    drawLine(guide, Offset(fl, ft + 2f * fh / 3f), Offset(fl + fw, ft + 2f * fh / 3f))
                }
            }

            // Top label.
            Text(
                text = "Crop - drag to move, pinch to zoom",
                color = Color(0xFFEDE8E6),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            )

            // Bottom controls: cancel or apply.
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
                    // Turn the on-screen frame into a fraction of the picture.
                    val left = ((iw * scale / 2f - fw / 2f - tx) / (scale * iw)).coerceIn(0f, 1f)
                    val top = ((ih * scale / 2f - fh / 2f - ty) / (scale * ih)).coerceIn(0f, 1f)
                    val right = (left + fw / (scale * iw)).coerceIn(0f, 1f)
                    val bottom = (top + fh / (scale * ih)).coerceIn(0f, 1f)
                    onApply(NormRect(left, top, right, bottom))
                }) {
                    Icon(Icons.Filled.Check, contentDescription = "Apply crop", tint = Color(0xFFD65A5A))
                }
            }
        }
    }
}
