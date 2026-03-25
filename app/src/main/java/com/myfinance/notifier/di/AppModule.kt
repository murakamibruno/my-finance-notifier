package com.myfinance.notifier.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.myfinance.notifier.data.local.AppDatabase
import com.myfinance.notifier.data.local.NotificationLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "myfinance_notifier.db"
        ).build()

    @Provides
    fun provideNotificationLogDao(database: AppDatabase): NotificationLogDao =
        database.notificationLogDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
