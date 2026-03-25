package com.myfinance.notifier.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myfinance.notifier.data.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RetryWorker"
        const val KEY_LOG_ID = "log_id"
    }

    override suspend fun doWork(): Result {
        return try {
            val logId = inputData.getLong(KEY_LOG_ID, -1L)
            if (logId != -1L) {
                Log.d(TAG, "Retrying single notification: $logId")
                notificationRepository.retrySingle(logId)
            }
            notificationRepository.cleanupOldLogs()
            Log.d(TAG, "Retry completed")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Retry failed", e)
            Result.retry()
        }
    }
}
