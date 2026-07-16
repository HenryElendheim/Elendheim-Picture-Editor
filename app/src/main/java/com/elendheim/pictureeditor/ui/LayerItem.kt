package com.elendheim.pictureeditor.ui

import android.graphics.Bitmap
import android.net.Uri
import com.elendheim.pictureeditor.model.NormPoint

/**
 * One added picture placed over the base image. The preview bitmap is used on
 * screen; the uri is kept so the full resolution version can be reloaded at
 * export. Position and size are fractions of the base so placement is the same
 * at any resolution.
 */
data class LayerItem(
    val id: String,
    val uri: Uri,
    val bitmap: Bitmap,
    val center: NormPoint = NormPoint(0.5f, 0.5f),
    val scale: Float = 0.5f,
    val rotationDeg: Float = 0f
)
