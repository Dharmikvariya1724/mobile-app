package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.example.data.database.VaultDatabase
import com.example.data.repository.MediaRepository
import com.example.data.repository.VaultRepository
import com.example.security.VaultSession
import com.example.security.VaultStorageManager

class VaultApplication : Application(), Application.ActivityLifecycleCallbacks {

    lateinit var database: VaultDatabase
        private set

    lateinit var vaultRepository: VaultRepository
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = VaultDatabase.getInstance(this)
        vaultRepository = VaultRepository(database)
        mediaRepository = MediaRepository(database)

        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Activity initialized
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
    }

    override fun onActivityResumed(activity: Activity) {
        VaultSession.recordActivity()
    }

    override fun onActivityPaused(activity: Activity) {
        VaultSession.recordActivity()
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount <= 0) {
            startedActivityCount = 0
            // App went to the background
            VaultSession.checkAutoLock()
            VaultStorageManager.clearMemoryCache()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (startedActivityCount <= 0) {
            VaultSession.lock()
            VaultStorageManager.clearMemoryCache()
        }
    }

    companion object {
        lateinit var instance: VaultApplication
            private set
    }
}
