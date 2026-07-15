package com.elendheim.pictureeditor.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/** The two output formats the editor can write. */
enum class ExportFormat(val label: String, val mime: String, val ext: String) {
    JPEG("JPEG", "image/jpeg", "jpg"),
    PNG("PNG", "image/png", "png")
}

/**
 * Writes the finished image into the phone's gallery as a brand new file. The
 * original photo is never touched. Files land in Pictures / Elendheim Picture
 * Editor so they are easy to find.
 */
object GalleryExporter {

    private const val ALBUM = "Elendheim Picture Editor"

    /**
     * Save the bitmap and return the new file's uri, or null if it failed.
     * On Android 10 and up this needs no permission at all thanks to scoped
     * storage; older versions use the classic public Pictures folder.
     */
    fun save(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        fileStem: String
    ): Uri? {
        val name = "$fileStem.${format.ext}"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(context, bitmap, format, quality, name)
        } else {
            saveLegacy(context, bitmap, format, quality, name)
        }
    }

    // Android 10+ -> MediaStore with a relative path, no permission required.
    private fun saveScoped(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        name: String
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/" + ALBUM
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(compressFormat(format), quality, out)
            } ?: return null
            // Clear the pending flag so the file becomes visible to the gallery.
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrElse {
            resolver.delete(uri, null, null)
            null
        }
    }

    // Android 9 and below -> write to the public Pictures folder, then index it.
    private fun saveLegacy(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        name: String
    ): Uri? = runCatching {
        @Suppress("DEPRECATION")
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val albumDir = File(picturesDir, ALBUM).apply { mkdirs() }
        val outFile = File(albumDir, name)
        FileOutputStream(outFile).use { out ->
            bitmap.compress(compressFormat(format), quality, out)
        }
        val values = ContentValues().apply {
            @Suppress("DEPRECATION")
            put(MediaStore.Images.Media.DATA, outFile.absolutePath)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }.getOrNull()

    private fun compressFormat(format: ExportFormat): Bitmap.CompressFormat = when (format) {
        ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
        ExportFormat.PNG -> Bitmap.CompressFormat.PNG
    }
}
