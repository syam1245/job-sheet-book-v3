package com.example.jobsheetbook.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtil {
    fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String): String? {
        val file = File(context.filesDir, fileName)
        var out: FileOutputStream? = null
        return try {
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            out?.close()
        }
    }
}
