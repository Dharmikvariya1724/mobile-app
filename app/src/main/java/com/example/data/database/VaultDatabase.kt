package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaEntity
import com.example.data.model.VaultConfigEntity

@Database(
    entities = [
        MediaEntity::class,
        AlbumEntity::class,
        VaultConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao
    abstract fun albumDao(): AlbumDao
    abstract fun vaultConfigDao(): VaultConfigDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "private_vault.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
