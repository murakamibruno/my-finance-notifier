package com.myfinance.notifier.data.repository

import com.myfinance.notifier.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: SettingsDataStore
) {
    val webhookUrl: Flow<String> = dataStore.webhookUrl
    val enabledBanks: Flow<Set<String>> = dataStore.enabledBanks

    suspend fun saveWebhookUrl(url: String) = dataStore.setWebhookUrl(url)
    suspend fun saveEnabledBanks(banks: Set<String>) = dataStore.setEnabledBanks(banks)

    suspend fun getWebhookUrlSync() = dataStore.getWebhookUrlSync()
    suspend fun getEnabledBanksSync() = dataStore.getEnabledBanksSync()
}
