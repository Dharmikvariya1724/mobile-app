package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaType {
    PHOTO,
    VIDEO
}

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    SIZE_DESC("Largest first"),
    SIZE_ASC("Smallest first"),
    NAME_ASC("Name (A-Z)")
}

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["albumId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["mediaType"]),
        Index(value = ["createdAt"])
    ]
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uuid: String,
    val encryptedFileName: String,
    val originalDisplayName: String,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val mimeType: String,
    val fileSize: Long,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val importedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val albumId: Long? = null,
    val encryptedThumbName: String? = null
)

@Entity(
    tableName = "albums",
    indices = [Index(value = ["name"], unique = true)]
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val coverMediaId: Long? = null
)

@Entity(tableName = "vault_config")
data class VaultConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val isInitialized: Boolean = false,
    val passwordSalt: String = "",
    val passwordVerifier: String = "",
    val iterations: Int = 120_000,
    val encryptedMasterKey: String = "",
    val masterKeyIv: String = "",
    val biometricEnabled: Boolean = false,
    val autoLockTimeoutSeconds: Int = 30,
    val failedAttempts: Int = 0,
    val lockoutUntilTimestamp: Long = 0L
)
