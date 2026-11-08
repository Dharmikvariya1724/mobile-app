package com.example.security

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.database.VaultDatabase
import com.example.data.model.MediaEntity
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.UUID

object VaultStorageManager {

    private const val MEDIA_DIR = "vault_media"
    private const val THUMB_DIR = "vault_thumbs"
    private const val EXPORT_DIR = "export_cache"

    // In-memory cache for decrypted thumbnails to provide silky-smooth 60fps scrolling
    private val thumbnailCache = LruCache<String, ImageBitmap>(150)

    fun clearMemoryCache() {
        thumbnailCache.evictAll()
    }

    fun getMediaDirectory(context: Context): File {
        val dir = File(context.filesDir, MEDIA_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getThumbDirectory(context: Context): File {
        val dir = File(context.filesDir, THUMB_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExportCacheDirectory(context: Context): File {
        val dir = File(context.cacheDir, EXPORT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getEncryptedMediaFile(context: Context, fileName: String): File {
        return File(getMediaDirectory(context), fileName)
    }

    fun getEncryptedThumbFile(context: Context, thumbName: String): File {
        return File(getThumbDirectory(context), thumbName)
    }

    /**
     * Imports a photo or video into the vault.
     * Content is encrypted with AES-256-GCM and stored in private internal storage.
     */
    suspend fun importMedia(
        context: Context,
        uri: Uri,
        albumId: Long? = null,
        onProgress: ((Float) -> Unit)? = null
    ): MediaEntity = withContext(Dispatchers.IO) {
        val masterKey = VaultSession.getActiveKey()
            ?: throw IllegalStateException("Vault is locked. Unlock before importing.")

        val contentResolver = context.contentResolver
        val (displayName, size) = queryFileInfo(contentResolver, uri)
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val isVideo = mimeType.startsWith("video/")

        val uuid = UUID.randomUUID().toString()
        val encryptedFileName = "$uuid.enc"
        val destinationFile = File(getMediaDirectory(context), encryptedFileName)

        // 1. Encrypt media chunk by chunk
        contentResolver.openInputStream(uri)?.use { inputStream ->
            CryptoManager.encryptStreamChunked(
                inputStream = inputStream,
                totalPlainSize = size,
                destinationFile = destinationFile,
                key = masterKey,
                onProgress = onProgress
            )
        } ?: throw IllegalStateException("Unable to open input stream for $uri")

        // 2. Generate and encrypt thumbnail
        val thumbFileName = "$uuid.thumb"
        val thumbFile = File(getThumbDirectory(context), thumbFileName)
        val (width, height, durationMs) = generateAndSaveThumbnail(
            context = context,
            uri = uri,
            isVideo = isVideo,
            thumbDestination = thumbFile
        )

        // 3. Construct entity
        val entity = MediaEntity(
            uuid = uuid,
            encryptedFileName = encryptedFileName,
            originalDisplayName = displayName,
            mediaType = if (isVideo) MediaType.VIDEO.name else MediaType.PHOTO.name,
            mimeType = mimeType,
            fileSize = destinationFile.length(),
            durationMs = durationMs,
            width = width,
            height = height,
            createdAt = System.currentTimeMillis(),
            importedAt = System.currentTimeMillis(),
            albumId = albumId,
            encryptedThumbName = thumbFileName
        )

        val id = VaultDatabase.getInstance(context).mediaDao().insert(entity)
        entity.copy(id = id)
    }

    /**
     * Loads and decrypts a thumbnail for list/grid display.
     * Cached in memory for smooth performance.
     */
    suspend fun loadThumbnail(context: Context, thumbName: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        if (thumbName == null) return@withContext null
        thumbnailCache.get(thumbName)?.let { return@withContext it }

        val masterKey = VaultSession.getActiveKey() ?: return@withContext null
        val thumbFile = getEncryptedThumbFile(context, thumbName)
        if (!thumbFile.exists()) return@withContext null

        try {
            val encryptedBytes = thumbFile.readBytes()
            val decryptedBytes = CryptoManager.decryptBytes(encryptedBytes, masterKey)
            val bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size) ?: return@withContext null
            val imageBitmap = bitmap.asImageBitmap()
            thumbnailCache.put(thumbName, imageBitmap)
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decrypts a full photo into memory for full-screen display.
     */
    suspend fun loadFullPhoto(context: Context, fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        val masterKey = VaultSession.getActiveKey() ?: return@withContext null
        val file = getEncryptedMediaFile(context, fileName)
        if (!file.exists()) return@withContext null

        try {
            val outputStream = ByteArrayOutputStream()
            CryptoManager.decryptStreamChunked(file, outputStream, masterKey)
            val bytes = outputStream.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Exports an encrypted media file to a destination OutputStream (e.g. SAF or MediaStore).
     */
    suspend fun exportMedia(
        context: Context,
        fileName: String,
        destinationStream: OutputStream,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val masterKey = VaultSession.getActiveKey()
            ?: throw IllegalStateException("Vault is locked")
        val file = getEncryptedMediaFile(context, fileName)
        CryptoManager.decryptStreamChunked(file, destinationStream, masterKey, onProgress)
    }

    /**
     * Decrypts media into a temporary file in the app-private cache folder for sharing.
     */
    suspend fun prepareMediaForSharing(context: Context, media: MediaEntity): File = withContext(Dispatchers.IO) {
        val masterKey = VaultSession.getActiveKey()
            ?: throw IllegalStateException("Vault is locked")
        val exportDir = getExportCacheDirectory(context)
        val extension = when {
            media.mediaType == MediaType.VIDEO.name -> ".mp4"
            media.mimeType.contains("png") -> ".png"
            else -> ".jpg"
        }
        val safeName = "shared_${media.uuid}$extension"
        val tempFile = File(exportDir, safeName)
        if (!tempFile.exists()) {
            tempFile.outputStream().use { out ->
                val sourceFile = getEncryptedMediaFile(context, media.encryptedFileName)
                CryptoManager.decryptStreamChunked(sourceFile, out, masterKey)
            }
        }
        tempFile
    }

    /**
     * Deletes media file and its thumbnail from internal storage.
     */
    suspend fun deleteEncryptedFiles(context: Context, media: MediaEntity) = withContext(Dispatchers.IO) {
        getEncryptedMediaFile(context, media.encryptedFileName).delete()
        media.encryptedThumbName?.let {
            getEncryptedThumbFile(context, it).delete()
            thumbnailCache.remove(it)
        }
    }

    /**
     * Completely wipes all vault media and thumbnails.
     */
    suspend fun wipeAllMediaFiles(context: Context) = withContext(Dispatchers.IO) {
        clearMemoryCache()
        getMediaDirectory(context).listFiles()?.forEach { it.delete() }
        getThumbDirectory(context).listFiles()?.forEach { it.delete() }
        getExportCacheDirectory(context).listFiles()?.forEach { it.delete() }
    }

    private fun queryFileInfo(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = "media_${System.currentTimeMillis()}"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
        return Pair(name, size)
    }

    private fun generateAndSaveThumbnail(
        context: Context,
        uri: Uri,
        isVideo: Boolean,
        thumbDestination: File
    ): Triple<Int, Int, Long> {
        val masterKey = VaultSession.getActiveKey()!!
        var width = 0
        var height = 0
        var durationMs = 0L
        var rawBitmap: Bitmap? = null

        try {
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                rawBitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                width = w
                height = h
                retriever.release()
            } else {
                // Photo: read bounds first
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    width = options.outWidth
                    height = options.outHeight
                }
                // Decode scaled thumbnail
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val sampleSize = calculateInSampleSize(width, height, 300, 300)
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    rawBitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }

            val thumbBitmap = rawBitmap?.let { scaleCenterCrop(it, 256, 256) }
                ?: Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)

            val stream = ByteArrayOutputStream()
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val thumbBytes = stream.toByteArray()

            val encryptedThumb = CryptoManager.encryptBytes(thumbBytes, masterKey)
            thumbDestination.writeBytes(encryptedThumb)
        } catch (e: Exception) {
            // Write placeholder encrypted black bitmap if extraction failed
            val emptyBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.RGB_565)
            val stream = ByteArrayOutputStream()
            emptyBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
            val encryptedThumb = CryptoManager.encryptBytes(stream.toByteArray(), masterKey)
            thumbDestination.writeBytes(encryptedThumb)
        }

        return Triple(width, height, durationMs)
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun scaleCenterCrop(source: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val xScale = newWidth.toFloat() / source.width
        val yScale = newHeight.toFloat() / source.height
        val scale = maxOf(xScale, yScale)

        val scaledWidth = scale * source.width
        val scaledHeight = scale * source.height

        val left = (scaledWidth - newWidth) / 2
        val top = (scaledHeight - newHeight) / 2

        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth.toInt(), scaledHeight.toInt(), true)
        return Bitmap.createBitmap(scaledBitmap, left.toInt(), top.toInt(), newWidth, newHeight)
    }
}
