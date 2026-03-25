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
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Retrying failed notifications...")
            notificationRepository.retryFailed()
            notificationRepository.cleanupOldLogs()
            Log.d(TAG, "Retry completed")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Retry failed", e)
            Result.retry()
        }
    }
}
