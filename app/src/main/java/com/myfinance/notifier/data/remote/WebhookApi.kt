package com.myfinance.notifier.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface WebhookApi {

    @POST
    suspend fun sendNotification(
        @Url webhookUrl: String,
        @Body payload: WebhookPayload
    ): Response<Any>
}
