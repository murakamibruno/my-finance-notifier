package com.myfinance.notifier.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myfinance.notifier.domain.NotificationLog

@Entity(tableName = "notification_log")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val banco: String,
    val texto: String,
    val dataHora: Long,
    val status: String,
    val httpStatus: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    fun toDomain() = NotificationLog(
        id = id,
        banco = banco,
        texto = texto,
        dataHora = dataHora,
        status = status,
        httpStatus = httpStatus,
        createdAt = createdAt,
        retryCount = retryCount
    )
}
