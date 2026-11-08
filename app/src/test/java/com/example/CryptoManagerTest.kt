package com.example

import com.example.security.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

class CryptoManagerTest {

    @Test
    fun testKeyGenerationAndDerivation() {
        val masterKey = CryptoManager.generateMasterKey()
        assertNotNull(masterKey)
        assertEquals("AES", masterKey.algorithm)
        assertEquals(32, masterKey.encoded.size) // 256 bits

        val salt = CryptoManager.generateSalt()
        assertEquals(32, salt.size)

        val kek1 = CryptoManager.deriveKey("1234".toCharArray(), salt, 1000)
        val kek2 = CryptoManager.deriveKey("1234".toCharArray(), salt, 1000)
        val kekWrong = CryptoManager.deriveKey("9999".toCharArray(), salt, 1000)

        assertArrayEquals(kek1.encoded, kek2.encoded)
        assertFalse(kek1.encoded.contentEquals(kekWrong.encoded))
    }

    @Test
    fun testMasterKeyWrappingAndUnwrapping() {
        val masterKey = CryptoManager.generateMasterKey()
        val salt = CryptoManager.generateSalt()
        val kek = CryptoManager.deriveKey("5678".toCharArray(), salt, 1000)

        val (encryptedKey, iv) = CryptoManager.encryptMasterKey(masterKey, kek)
        assertNotNull(encryptedKey)
        assertEquals(12, iv.size)

        val unwrappedKey = CryptoManager.decryptMasterKey(encryptedKey, iv, kek)
        assertArrayEquals(masterKey.encoded, unwrappedKey.encoded)
    }

    @Test
    fun testBytesEncryptionDecryption() {
        val masterKey = CryptoManager.generateMasterKey()
        val originalText = "TopSecretPrivatePhotoData_2026".toByteArray(Charsets.UTF_8)

        val encrypted = CryptoManager.encryptBytes(originalText, masterKey)
        assertTrue(encrypted.size > originalText.size)

        val decrypted = CryptoManager.decryptBytes(encrypted, masterKey)
        assertArrayEquals(originalText, decrypted)
    }

    @Test
    fun testTamperedCiphertextDetection() {
        val masterKey = CryptoManager.generateMasterKey()
        val originalText = "ConfidentialVaultContent".toByteArray(Charsets.UTF_8)

        val encrypted = CryptoManager.encryptBytes(originalText, masterKey)

        // Modify 1 byte in the ciphertext payload
        val tampered = encrypted.clone()
        val targetIndex = tampered.size - 5
        tampered[targetIndex] = (tampered[targetIndex].toInt() xor 0xFF).toByte()

        try {
            CryptoManager.decryptBytes(tampered, masterKey)
            fail("Expected decryption failure due to tampered ciphertext or authentication tag mismatch")
        } catch (e: Exception) {
            // Success: GCM Authentication tag correctly rejected tampered ciphertext
            assertTrue(e.message?.contains("Tag mismatch", ignoreCase = true) == true ||
                    e is javax.crypto.AEADBadTagException ||
                    e is java.security.GeneralSecurityException)
        }
    }

    @Test
    fun testChunkedStreamEncryptionAndDecryption() {
        val masterKey = CryptoManager.generateMasterKey()
        val testData = ByteArray(150 * 1024) { (it % 256).toByte() } // 150 KB > 64KB chunk size
        val tempEncryptedFile = Files.createTempFile("vault_test", ".enc").toFile()

        try {
            // Encrypt stream
            CryptoManager.encryptStreamChunked(
                inputStream = ByteArrayInputStream(testData),
                totalPlainSize = testData.size.toLong(),
                destinationFile = tempEncryptedFile,
                key = masterKey
            )

            assertTrue(tempEncryptedFile.exists())
            assertTrue(tempEncryptedFile.length() > testData.size)

            // Decrypt stream
            val outputStream = ByteArrayOutputStream()
            CryptoManager.decryptStreamChunked(tempEncryptedFile, outputStream, masterKey)

            assertArrayEquals(testData, outputStream.toByteArray())

            // Test Random Range Decryption (Simulates ExoPlayer seekable playback)
            val offset = 70_000L // Spans across chunk boundary
            val length = 10_000
            val rangeBytes = CryptoManager.readDecryptedRange(tempEncryptedFile, offset, length, masterKey)

            assertEquals(length, rangeBytes.size)
            val expectedSubArray = testData.copyOfRange(offset.toInt(), (offset + length).toInt())
            assertArrayEquals(expectedSubArray, rangeBytes)

        } finally {
            tempEncryptedFile.delete()
        }
    }
}
