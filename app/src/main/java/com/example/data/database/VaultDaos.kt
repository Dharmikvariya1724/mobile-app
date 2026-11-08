package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaEntity
import com.example.data.model.VaultConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY createdAt DESC")
    fun getMediaByType(type: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavorites(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE albumId = :albumId ORDER BY createdAt DESC")
    fun getMediaByAlbum(albumId: Long): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaEntity?

    @Query("SELECT * FROM media_items WHERE uuid = :uuid LIMIT 1")
    suspend fun getMediaByUuid(uuid: String): MediaEntity?

    @Query("SELECT * FROM media_items WHERE originalDisplayName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMedia(query: String): Flow<List<MediaEntity>>

    @Query("SELECT COUNT(*) FROM media_items")
    fun getMediaCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM media_items")
    fun getTotalVaultSize(): Flow<Long>

    @Query("SELECT COUNT(*) FROM media_items WHERE mediaType = 'PHOTO'")
    fun getPhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE mediaType = 'VIDEO'")
    fun getVideoCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaEntity>): List<Long>

    @Update
    suspend fun update(media: MediaEntity)

    @Delete
    suspend fun delete(media: MediaEntity)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_items")
    suspend fun deleteAll()

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET albumId = :albumId WHERE id IN (:ids)")
    suspend fun moveToAlbum(ids: List<Long>, albumId: Long?)

    @Query("UPDATE media_items SET albumId = NULL WHERE albumId = :albumId")
    suspend fun removeAlbumAssociation(albumId: Long)
}

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY createdAt ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id LIMIT 1")
    suspend fun getAlbumById(id: Long): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumEntity): Long

    @Update
    suspend fun update(album: AlbumEntity)

    @Delete
    suspend fun delete(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM albums")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM media_items WHERE albumId = :albumId")
    fun getMediaCountInAlbum(albumId: Long): Flow<Int>
}

@Dao
interface VaultConfigDao {

    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<VaultConfigEntity?>

    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): VaultConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: VaultConfigEntity)

    @Query("UPDATE vault_config SET failedAttempts = :attempts, lockoutUntilTimestamp = :lockoutTimestamp WHERE id = 1")
    suspend fun updateFailedAttempts(attempts: Int, lockoutTimestamp: Long)

    @Query("UPDATE vault_config SET biometricEnabled = :enabled WHERE id = 1")
    suspend fun updateBiometric(enabled: Boolean)

    @Query("UPDATE vault_config SET autoLockTimeoutSeconds = :seconds WHERE id = 1")
    suspend fun updateAutoLockTimeout(seconds: Int)

    @Query("DELETE FROM vault_config")
    suspend fun clearConfig()
}
