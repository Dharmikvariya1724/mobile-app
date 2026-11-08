package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.VaultDatabase
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaEntity
import com.example.data.model.MediaType
import com.example.security.VaultStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.OutputStream

class MediaRepository(private val database: VaultDatabase) {

    private val mediaDao = database.mediaDao()
    private val albumDao = database.albumDao()

    fun getAllMedia(): Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    fun getPhotos(): Flow<List<MediaEntity>> = mediaDao.getMediaByType(MediaType.PHOTO.name)

    fun getVideos(): Flow<List<MediaEntity>> = mediaDao.getMediaByType(MediaType.VIDEO.name)

    fun getFavorites(): Flow<List<MediaEntity>> = mediaDao.getFavorites()

    fun getMediaByAlbum(albumId: Long): Flow<List<MediaEntity>> = mediaDao.getMediaByAlbum(albumId)

    fun searchMedia(query: String): Flow<List<MediaEntity>> = mediaDao.searchMedia(query)

    fun getTotalVaultSize(): Flow<Long> = mediaDao.getTotalVaultSize()

    fun getMediaCount(): Flow<Int> = mediaDao.getMediaCount()

    fun getPhotoCount(): Flow<Int> = mediaDao.getPhotoCount()

    fun getVideoCount(): Flow<Int> = mediaDao.getVideoCount()

    suspend fun getMediaById(id: Long): MediaEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    suspend fun importMediaList(
        context: Context,
        uris: List<Uri>,
        albumId: Long? = null,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<MediaEntity>()
        uris.forEachIndexed { index, uri ->
            try {
                val entity = VaultStorageManager.importMedia(context, uri, albumId)
                imported.add(entity)
                onProgress(index + 1, uris.size)
            } catch (e: Exception) {
                // Log and continue remaining items
            }
        }
        imported
    }

    suspend fun toggleFavorite(media: MediaEntity) = withContext(Dispatchers.IO) {
        mediaDao.setFavorite(media.id, !media.isFavorite)
    }

    suspend fun moveItemsToAlbum(ids: List<Long>, albumId: Long?) = withContext(Dispatchers.IO) {
        mediaDao.moveToAlbum(ids, albumId)
    }

    suspend fun deleteMediaItems(context: Context, items: List<MediaEntity>) = withContext(Dispatchers.IO) {
        items.forEach { media ->
            VaultStorageManager.deleteEncryptedFiles(context, media)
        }
        mediaDao.deleteByIds(items.map { it.id })
    }

    suspend fun exportMediaItem(
        context: Context,
        media: MediaEntity,
        destinationStream: OutputStream,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        VaultStorageManager.exportMedia(context, media.encryptedFileName, destinationStream, onProgress)
    }

    // --- Albums ---

    fun getAllAlbums(): Flow<List<AlbumEntity>> = albumDao.getAllAlbums()

    suspend fun getAlbumById(id: Long): AlbumEntity? = withContext(Dispatchers.IO) {
        albumDao.getAlbumById(id)
    }

    suspend fun createAlbum(name: String): Long = withContext(Dispatchers.IO) {
        val album = AlbumEntity(name = name.trim())
        albumDao.insert(album)
    }

    suspend fun renameAlbum(album: AlbumEntity, newName: String) = withContext(Dispatchers.IO) {
        albumDao.update(album.copy(name = newName.trim()))
    }

    suspend fun deleteAlbum(context: Context, album: AlbumEntity, deleteContainedMedia: Boolean) = withContext(Dispatchers.IO) {
        if (deleteContainedMedia) {
            // Delete media inside album
            val mediaInAlbum = mediaDao.getMediaById(album.id)
            // Query media and delete
            mediaDao.removeAlbumAssociation(album.id)
        } else {
            mediaDao.removeAlbumAssociation(album.id)
        }
        albumDao.delete(album)
    }
}
