package com.myfinance.notifier.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myfinance.notifier.data.remote.WebhookApi
import com.myfinance.notifier.data.remote.WebhookPayload
import com.myfinance.notifier.data.repository.NotificationRepository
import com.myfinance.notifier.data.repository.SettingsRepository
import com.myfinance.notifier.domain.NotificationLog
import com.myfinance.notifier.service.NotificationCaptureService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository,
    private val webhookApi: WebhookApi
) : ViewModel() {

    val webhookUrl: StateFlow<String> = settingsRepository.webhookUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val enabledBanks: StateFlow<Set<String>> = settingsRepository.enabledBanks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val logs: StateFlow<List<NotificationLog>> = notificationRepository.logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testResult = MutableStateFlow<TestResult?>(null)
    val testResult: StateFlow<TestResult?> = _testResult.asStateFlow()

    private val _retryingIds = MutableStateFlow<Set<Long>>(emptySet())
    val retryingIds: StateFlow<Set<Long>> = _retryingIds.asStateFlow()

    private val _retryingAll = MutableStateFlow(false)
    val retryingAll: StateFlow<Boolean> = _retryingAll.asStateFlow()

    val isServiceRunning: StateFlow<Boolean> = NotificationCaptureService.isRunning

    fun saveWebhookUrl(url: String) {
        viewModelScope.launch { settingsRepository.saveWebhookUrl(url) }
    }

    fun toggleBank(bankName: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getEnabledBanksSync().toMutableSet()
            if (enabled) current.add(bankName) else current.remove(bankName)
            settingsRepository.saveEnabledBanks(current)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _testResult.value = TestResult.Loading
            try {
                val url = settingsRepository.getWebhookUrlSync()
                if (url.isBlank()) {
                    _testResult.value = TestResult.Error("URL do webhook não configurada")
                    return@launch
                }

                val testPayload = WebhookPayload(
                    banco = "bradesco",
                    texto = "Compra de R$ 100,00 APROVADA em LOJA TESTE, no Cartão final 1114.",
                    dataHora = System.currentTimeMillis()
                )

                val response = webhookApi.sendNotification(url, testPayload)
                _testResult.value = if (response.isSuccessful) {
                    TestResult.Success
                } else {
                    TestResult.Error("HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                _testResult.value = TestResult.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun retrySingle(logId: Long) {
        viewModelScope.launch {
            _retryingIds.value = _retryingIds.value + logId
            notificationRepository.retrySingle(logId)
            _retryingIds.value = _retryingIds.value - logId
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            _retryingAll.value = true
            notificationRepository.retryFailed()
            _retryingAll.value = false
        }
    }

    fun clearLogs() {
        viewModelScope.launch { notificationRepository.clearAll() }
    }

    sealed class TestResult {
        data object Loading : TestResult()
        data object Success : TestResult()
        data class Error(val message: String) : TestResult()
    }
}
