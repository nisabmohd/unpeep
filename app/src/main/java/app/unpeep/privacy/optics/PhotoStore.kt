package app.unpeep.privacy.optics

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Proof photos (CLAUDE.md › Behaviour rules): saved in the app's own storage, never in the gallery, and deleted with
 * their report. They leave the phone only when the user exports a report through the share sheet.
 */
class PhotoStore(context: Context) {
    private val dir = File(context.applicationContext.filesDir, DIR)

    /** Saves [bitmap] as a JPEG; returns its file, or null when the phone has no room. */
    suspend fun save(bitmap: Bitmap, at: Long): File? = withContext(Dispatchers.IO) {
        try {
            dir.mkdirs()
            val file = File(dir, "photo-$at.jpg")
            file.outputStream().use { if (!bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it)) return@withContext null }
            file
        } catch (e: IOException) {
            null
        }
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        // Only ever a file of our own folder.
        if (file.parentFile?.canonicalPath == dir.canonicalPath) file.delete()
    }

    private companion object {
        const val DIR = "photos"
        const val QUALITY = 90
    }
}
