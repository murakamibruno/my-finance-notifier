package com.myfinance.notifier.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NotificationLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationLogDao(): NotificationLogDao
}
