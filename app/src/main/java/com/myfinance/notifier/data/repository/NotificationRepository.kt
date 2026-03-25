package com.myfinance.notifier.data.repository

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.myfinance.notifier.data.local.NotificationLogDao
import com.myfinance.notifier.data.local.NotificationLogEntity
import com.myfinance.notifier.data.remote.WebhookApi
import com.myfinance.notifier.data.remote.WebhookPayload
import com.myfinance.notifier.domain.NotificationLog
import com.myfinance.notifier.service.RetryWorker
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val webhookApi: WebhookApi,
    private val dao: NotificationLogDao,
    private val settingsRepository: SettingsRepository,
    private val workManager: WorkManager
) {
    companion object {
        private const val TAG = "NotificationRepository"
    }

    val logs: Flow<List<NotificationLog>> = dao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun send(payload: WebhookPayload): Long {
        val entity = NotificationLogEntity(
            banco = payload.banco,
            texto = payload.texto.take(500),
            dataHora = payload.dataHora,
            status = NotificationLog.STATUS_PENDING
        )
        val logId = dao.insert(entity)

        try {
            val webhookUrl = settingsRepository.getWebhookUrlSync()
            if (webhookUrl.isBlank()) {
                Log.w(TAG, "Webhook URL not configured, marking as FAILED")
                withContext(NonCancellable) {
                    dao.updateStatus(logId, NotificationLog.STATUS_FAILED, null)
                }
                return logId
            }

            val response = webhookApi.sendNotification(webhookUrl, payload)
            withContext(NonCancellable) {
                if (response.isSuccessful) {
                    dao.updateStatus(logId, NotificationLog.STATUS_SENT, response.code())
                    Log.d(TAG, "Notification sent successfully: ${payload.banco}")
                } else {
                    dao.updateStatus(logId, NotificationLog.STATUS_FAILED, response.code())
                    Log.w(TAG, "Notification failed with HTTP ${response.code()}")
                    enqueueRetry(logId)
                }
            }
        } catch (e: Exception) {
            withContext(NonCancellable) {
                dao.updateStatus(logId, NotificationLog.STATUS_FAILED, null)
                Log.e(TAG, "Notification send error", e)
                enqueueRetry(logId)
            }
        }
        return logId
    }

    suspend fun retrySingle(logId: Long) {
        val entity = dao.getById(logId) ?: return
        val webhookUrl = settingsRepository.getWebhookUrlSync()
        try {
            val payload = WebhookPayload(entity.banco, entity.texto, entity.dataHora)
            val response = webhookApi.sendNotification(webhookUrl, payload)
            if (response.isSuccessful) {
                dao.updateStatus(entity.id, NotificationLog.STATUS_SENT, response.code())
            } else {
                dao.updateStatusWithRetry(entity.id, NotificationLog.STATUS_FAILED, response.code())
            }
        } catch (e: Exception) {
            dao.updateStatusWithRetry(entity.id, NotificationLog.STATUS_FAILED, null)
        }
    }

    suspend fun retryFailed() {
        val failed = dao.getFailed()
        val webhookUrl = settingsRepository.getWebhookUrlSync()
        for (entity in failed) {
            try {
                val payload = WebhookPayload(entity.banco, entity.texto, entity.dataHora)
                val response = webhookApi.sendNotification(webhookUrl, payload)

                if (response.isSuccessful) {
                    dao.updateStatus(entity.id, NotificationLog.STATUS_SENT, response.code())
                } else {
                    dao.updateStatusWithRetry(entity.id, NotificationLog.STATUS_FAILED, response.code())
                }
            } catch (e: Exception) {
                dao.updateStatusWithRetry(entity.id, NotificationLog.STATUS_FAILED, null)
            }
        }
    }

    suspend fun getLogById(id: Long): NotificationLog? {
        return dao.getById(id)?.toDomain()
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun cleanupOldLogs() {
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        dao.deleteOlderThan(thirtyDaysAgo)
    }

    fun enqueueRetry(logId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val retryWork = OneTimeWorkRequestBuilder<RetryWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(RetryWorker.KEY_LOG_ID to logId))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "retry_notification_$logId",
            ExistingWorkPolicy.KEEP,
            retryWork
        )
    }
}
