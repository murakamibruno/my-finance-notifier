package com.myfinance.notifier.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.myfinance.notifier.BuildConfig
import com.myfinance.notifier.data.local.SettingsDataStore
import com.myfinance.notifier.data.remote.WebhookPayload
import com.myfinance.notifier.data.repository.NotificationRepository
import com.myfinance.notifier.domain.BankApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class SmsCaptureReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var settingsDataStore: SettingsDataStore

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "SmsCaptureReceiver"
        private const val DEDUP_WINDOW_MS = 5_000L
        private val recentSms = ConcurrentHashMap<String, Long>()
        const val DEBUG_ACTION = "com.myfinance.notifier.DEBUG_SMS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when {
            BuildConfig.DEBUG && intent.action == DEBUG_ACTION -> handleDebugSms(intent)
            intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> handleRealSms(intent)
        }
    }

    private fun handleDebugSms(intent: Intent) {
        val address = intent.getStringExtra("address")?.takeIf { it.isNotBlank() } ?: return
        val body = resolveDebugBody(intent) ?: run {
            Log.w(TAG, "Debug SMS received with blank body — ignoring")
            return
        }
        val bankApp = BankApp.fromSenderAddress(address) ?: run {
            Log.w(TAG, "No BankApp found for debug address: $address")
            return
        }
        processSms(address, body, System.currentTimeMillis(), bankApp)
    }

    private fun resolveDebugBody(intent: Intent): String? {
        val b64 = intent.getStringExtra("body_b64")?.takeIf { it.isNotBlank() }
        if (b64 != null) {
            return runCatching {
                String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT))
            }.getOrNull()?.takeIf { it.isNotBlank() }
        }
        return intent.getStringExtra("body")?.takeIf { it.isNotBlank() }
    }

    private fun handleRealSms(intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val grouped = messages.groupBy { it.originatingAddress ?: "" }
        for ((address, parts) in grouped) {
            val bankApp = BankApp.fromSenderAddress(address) ?: continue
            val fullText = parts.joinToString("") { it.messageBody ?: "" }
            if (fullText.isBlank()) continue
            val timestamp = parts.first().timestampMillis
            processSms(address, fullText, timestamp, bankApp)
        }
    }

    private fun processSms(address: String, text: String, timestamp: Long, bankApp: BankApp) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                val enabledBanks = settingsDataStore.getEnabledBanksSync()
                if (bankApp.name !in enabledBanks) return@launch

                if (isDuplicate(address, text, timestamp)) {
                    Log.d(TAG, "Duplicate SMS ignored: ${bankApp.displayName}")
                    return@launch
                }

                val payload = WebhookPayload(
                    banco = bankApp.bancoKey,
                    texto = text,
                    dataHora = timestamp
                )

                Log.d(TAG, "Captured SMS from ${bankApp.displayName}: ${text.take(50)}...")
                notificationRepository.send(payload)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isDuplicate(address: String, text: String, timestamp: Long): Boolean {
        cleanupOldEntries()
        val key = "$address|$text|$timestamp".hashCode().toString()
        return recentSms.putIfAbsent(key, System.currentTimeMillis()) != null
    }

    private fun cleanupOldEntries() {
        val now = System.currentTimeMillis()
        recentSms.entries.removeIf { now - it.value > DEDUP_WINDOW_MS }
    }
}
