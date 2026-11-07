package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Metadata for a user-imported focus wallpaper stored durably in app-private storage.
 */
data class FocusWallpaperItem(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val displayName: String,
    val dateAddedMillis: Long = System.currentTimeMillis()
)

/**
 * Manager for the persistent personal focus background gallery.
 * Copies imported images into app-private storage so they remain available offline,
 * survive tablet reboots, app updates, and rotation without permission revocation.
 */
object FocusWallpaperManager {

    private const val TAG = "FocusWallpaperManager"
    private const val WALLPAPER_DIR = "focus_backgrounds"
    private const val SETTINGS_KEY = "focus_wallpapers_list"
    private const val MAX_DIMENSION = 2560 // Downsamples extreme camera photos to 2.5K max to prevent OOM

    private val _wallpapers = MutableStateFlow<List<FocusWallpaperItem>>(emptyList())
    val wallpapers: StateFlow<List<FocusWallpaperItem>> = _wallpapers.asStateFlow()

    private val _activeWallpaper = MutableStateFlow<FocusWallpaperItem?>(null)
    val activeWallpaper: StateFlow<FocusWallpaperItem?> = _activeWallpaper.asStateFlow()

    fun loadWallpapers(context: Context, jsonString: String?) {
        val list = mutableListOf<FocusWallpaperItem>()
        if (!jsonString.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val path = obj.getString("filePath")
                    val file = File(path)
                    if (file.exists()) {
                        list.add(
                            FocusWallpaperItem(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                filePath = path,
                                displayName = obj.optString("displayName", "Wallpaper"),
                                dateAddedMillis = obj.optLong("dateAddedMillis", System.currentTimeMillis())
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing wallpapers json: ${e.message}")
            }
        }
        _wallpapers.value = list
    }

    fun serializeWallpapers(): String {
        val array = JSONArray()
        for (item in _wallpapers.value) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("filePath", item.filePath)
                put("displayName", item.displayName)
                put("dateAddedMillis", item.dateAddedMillis)
            }
            array.put(obj)
        }
        return array.toString()
    }

    suspend fun importWallpapers(context: Context, uris: List<Uri>): List<FocusWallpaperItem> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, WALLPAPER_DIR).apply { mkdirs() }
        val imported = mutableListOf<FocusWallpaperItem>()

        for (uri in uris) {
            try {
                val filename = "bg_${UUID.randomUUID()}.jpg"
                val destFile = File(dir, filename)

                val displayName = resolveFileName(context, uri) ?: "Wallpaper ${imported.size + 1}"

                context.contentResolver.openInputStream(uri)?.use { input ->
                    saveAndScaleBitmap(input, destFile)
                }

                if (destFile.exists() && destFile.length() > 0) {
                    val item = FocusWallpaperItem(
                        filePath = destFile.absolutePath,
                        displayName = displayName
                    )
                    imported.add(item)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import wallpaper from $uri: ${e.message}", e)
            }
        }

        if (imported.isNotEmpty()) {
            val updated = _wallpapers.value + imported
            _wallpapers.value = updated
        }
        return@withContext imported
    }

    suspend fun deleteWallpaper(item: FocusWallpaperItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
            val updated = _wallpapers.value.filter { it.id != item.id }
            _wallpapers.value = updated
            if (_activeWallpaper.value?.id == item.id) {
                _activeWallpaper.value = updated.firstOrNull()
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting wallpaper: ${e.message}")
            return@withContext false
        }
    }

    suspend fun clearAllWallpapers(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, WALLPAPER_DIR)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            _wallpapers.value = emptyList()
            _activeWallpaper.value = null
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing wallpapers: ${e.message}")
            return@withContext false
        }
    }

    fun setActiveWallpaper(item: FocusWallpaperItem?) {
        _activeWallpaper.value = item
    }

    private fun saveAndScaleBitmap(input: InputStream, destFile: File) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val tempBytes = input.readBytes()
        BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)

        var sampleSize = 1
        val maxDim = maxOf(options.outWidth, options.outHeight)
        if (maxDim > MAX_DIMENSION) {
            sampleSize = maxDim / MAX_DIMENSION
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = maxOf(1, sampleSize)
            inPreferredConfig = Bitmap.Config.RGB_565 // Low-memory footprint
        }

        val bitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)
        if (bitmap != null) {
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            bitmap.recycle()
        }
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }
}
