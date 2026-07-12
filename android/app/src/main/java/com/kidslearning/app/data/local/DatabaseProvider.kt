package com.kidslearning.app.data.local

import android.content.Context
import androidx.room.Room

/** Single Room database instance for the app. */
object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kids_learning.db",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
}
