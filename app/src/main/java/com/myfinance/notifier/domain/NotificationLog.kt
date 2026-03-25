package com.myfinance.notifier.domain

data class NotificationLog(
    val id: Long,
    val banco: String,
    val texto: String,
    val dataHora: Long,
    val status: String,
    val httpStatus: Int?,
    val createdAt: Long,
    val retryCount: Int
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
    }
}
