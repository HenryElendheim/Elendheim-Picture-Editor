package com.elendheim.pictureeditor.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/**
 * Loads photos from the picker. Two jobs: keep the preview small enough to be
 * snappy, and respect the photo's EXIF rotation so portraits are not sideways.
 * The full size loader is used only at export time.
 */
object ImageLoader {

    /** A downscaled copy for the live preview. maxDim caps the longest edge. */
    fun loadPreview(context: Context, uri: Uri, maxDim: Int = 1600): Bitmap? =
        load(context, uri, maxDim)

    /** The full resolution copy for export, still capped to avoid running out of memory. */
    fun loadFull(context: Context, uri: Uri, maxDim: Int = 4096): Bitmap? =
        load(context, uri, maxDim)

    private fun load(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val resolver = context.contentResolver

        // First pass: read only the dimensions so we can pick a sample size.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / sample > maxDim) sample *= 2

        // Second pass: actually decode at the reduced size.
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        return applyExifRotation(context, uri, decoded)
    }

    // Photos often carry a rotation flag instead of rotated pixels; honour it.
    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }
}
