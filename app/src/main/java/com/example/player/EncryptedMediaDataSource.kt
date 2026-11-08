package com.example.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.example.security.CryptoManager
import com.example.security.VaultSession
import java.io.File
import javax.crypto.SecretKey

/**
 * Custom Media3 ExoPlayer DataSource that streams and decrypts chunked AES-256-GCM
 * encrypted video files on-the-fly directly into memory buffers.
 *
 * Benefits:
 * - Zero plaintext video files written to disk or shared caches
 * - Constant low-memory usage (< 256 KB) even for multi-gigabyte 4K videos
 * - Full random-access seeking supported via chunk-addressed AES-GCM
 */
class EncryptedMediaDataSource(
    private val encryptedFile: File,
    private val key: SecretKey
) : BaseDataSource(/* isNetwork = */ false) {

    private var uri: Uri? = null
    private var opened = false
    private var currentPosition: Long = 0L
    private var bytesRemaining: Long = 0L
    private var totalPlainSize: Long = 0L

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        totalPlainSize = CryptoManager.getPlainSize(encryptedFile)
        currentPosition = dataSpec.position

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            totalPlainSize - currentPosition
        }

        if (bytesRemaining < 0) {
            bytesRemaining = 0
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = length.toLong().coerceAtMost(bytesRemaining).toInt()
        val decryptedBytes = CryptoManager.readDecryptedRange(
            encryptedFile = encryptedFile,
            offset = currentPosition,
            length = bytesToRead,
            key = key
        )

        if (decryptedBytes.isEmpty()) {
            return C.RESULT_END_OF_INPUT
        }

        System.arraycopy(decryptedBytes, 0, buffer, offset, decryptedBytes.size)
        currentPosition += decryptedBytes.size
        bytesRemaining -= decryptedBytes.size

        bytesTransferred(decryptedBytes.size)
        return decryptedBytes.size
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    class Factory(
        private val encryptedFile: File
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            val key = VaultSession.getActiveKey()
                ?: throw IllegalStateException("Vault is locked. Cannot play encrypted video.")
            return EncryptedMediaDataSource(encryptedFile, key)
        }
    }
}
