package com.vyrncore.palestra.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Shares a rendered card (workout result, streak, ...) as a PNG via the platform share sheet -
 * the same FileProvider setup [CsvExporter] already uses for CSV exports. */
object ImageShareUtil {

    fun shareBitmap(context: Context, bitmap: Bitmap, chooserTitle: String = "Condividi") {
        val imagesDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(imagesDir, "risultato_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
