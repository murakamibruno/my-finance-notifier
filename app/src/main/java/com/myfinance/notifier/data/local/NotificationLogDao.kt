package com.myfinance.notifier.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {

    @Query("SELECT * FROM notification_log ORDER BY createdAt DESC")
    fun getAll(): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_log WHERE status = 'FAILED'")
    suspend fun getFailed(): List<NotificationLogEntity>

    @Insert
    suspend fun insert(entity: NotificationLogEntity): Long

    @Query("UPDATE notification_log SET status = :status, httpStatus = :httpStatus WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, httpStatus: Int?)

    @Query("UPDATE notification_log SET status = :status, httpStatus = :httpStatus, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateStatusWithRetry(id: Long, status: String, httpStatus: Int?)

    @Query("DELETE FROM notification_log WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM notification_log")
    suspend fun deleteAll()
}
