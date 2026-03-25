package com.myfinance.notifier.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.myfinance.notifier.MyFinanceNotifierApp
import com.myfinance.notifier.R
import com.myfinance.notifier.data.local.SettingsDataStore
import com.myfinance.notifier.data.remote.WebhookPayload
import com.myfinance.notifier.data.repository.NotificationRepository
import com.myfinance.notifier.domain.BankApp
import com.myfinance.notifier.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class NotificationCaptureService : NotificationListenerService() {

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var settingsDataStore: SettingsDataStore

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val recentNotifications = ConcurrentHashMap<String, Long>()

    companion object {
        private const val TAG = "NotificationCapture"
        private const val DEDUP_WINDOW_MS = 5_000L
        private const val FOREGROUND_ID = 1
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        startForegroundMonitoring()
        _isRunning.value = true
        Log.i(TAG, "NotificationListenerService connected")
    }

    private fun startForegroundMonitoring() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, MyFinanceNotifierApp.MONITORING_CHANNEL_ID)
            .setContentTitle(getString(R.string.monitoring_notification_title))
            .setContentText(getString(R.string.monitoring_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isRunning.value = false
        Log.i(TAG, "NotificationListenerService disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val bankApp = BankApp.fromPackageName(sbn.packageName) ?: return

        scope.launch {
            try {
                val enabledBanks = settingsDataStore.getEnabledBanksSync()
                if (bankApp.name !in enabledBanks) return@launch

                val texto = sbn.notification.extras
                    ?.getString(Notification.EXTRA_TEXT)
                    ?: return@launch

                if (isDuplicate(sbn.packageName, texto, sbn.postTime)) {
                    Log.d(TAG, "Duplicate notification ignored: ${bankApp.displayName}")
                    return@launch
                }

                val payload = WebhookPayload(
                    banco = bankApp.bancoKey,
                    texto = texto,
                    dataHora = sbn.postTime
                )

                Log.d(TAG, "Captured notification from ${bankApp.displayName}: ${texto.take(50)}...")
                notificationRepository.send(payload)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    private fun isDuplicate(packageName: String, texto: String, timestamp: Long): Boolean {
        cleanupOldEntries()
        val key = "$packageName|$texto|$timestamp".hashCode().toString()
        val existing = recentNotifications.putIfAbsent(key, System.currentTimeMillis())
        return existing != null
    }

    private fun cleanupOldEntries() {
        val now = System.currentTimeMillis()
        recentNotifications.entries.removeIf { now - it.value > DEDUP_WINDOW_MS }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        _isRunning.value = false
    }
}
