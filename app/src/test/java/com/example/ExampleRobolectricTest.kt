package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.VaultDatabase
import com.example.data.model.MediaEntity
import com.example.data.model.MediaType
import com.example.security.VaultSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PrivateVault", appName)
    }

    @Test
    fun `verify database operations and entities`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = VaultDatabase.getInstance(context)

        val media = MediaEntity(
            uuid = "test-uuid-1",
            encryptedFileName = "test-uuid-1.enc",
            originalDisplayName = "secure_photo.jpg",
            mediaType = MediaType.PHOTO.name,
            mimeType = "image/jpeg",
            fileSize = 1024L
        )

        val id = db.mediaDao().insert(media)
        assertTrue(id > 0)

        val fetched = db.mediaDao().getMediaById(id)
        assertNotNull(fetched)
        assertEquals("secure_photo.jpg", fetched?.originalDisplayName)

        val all = db.mediaDao().getAllMedia().first()
        assertTrue(all.isNotEmpty())

        db.mediaDao().deleteByIds(listOf(id))
        val afterDelete = db.mediaDao().getMediaById(id)
        assertEquals(null, afterDelete)
    }

    @Test
    fun `verify vault session lock behavior`() {
        assertFalse(VaultSession.isUnlocked.value)
        assertEquals(null, VaultSession.getActiveKey())

        val mockKey = javax.crypto.spec.SecretKeySpec(ByteArray(32) { 0x01 }, "AES")
        VaultSession.unlock(mockKey)
        assertTrue(VaultSession.isUnlocked.value)
        assertNotNull(VaultSession.getActiveKey())

        VaultSession.lock()
        assertFalse(VaultSession.isUnlocked.value)
        assertEquals(null, VaultSession.getActiveKey())
    }
}
