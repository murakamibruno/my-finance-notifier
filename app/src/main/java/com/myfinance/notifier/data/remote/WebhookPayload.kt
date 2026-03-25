package com.myfinance.notifier.data.remote

data class WebhookPayload(
    val banco: String,
    val texto: String,
    val dataHora: Long
)
