package com.myfinance.notifier.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val ENABLED_BANKS = stringSetPreferencesKey("enabled_banks")
    }

    val webhookUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WEBHOOK_URL] ?: ""
    }

    val enabledBanks: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENABLED_BANKS] ?: emptySet()
    }

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WEBHOOK_URL] = url.trim()
        }
    }

    suspend fun setEnabledBanks(banks: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLED_BANKS] = banks
        }
    }

    suspend fun getWebhookUrlSync(): String =
        context.dataStore.data.first()[Keys.WEBHOOK_URL] ?: ""

    suspend fun getEnabledBanksSync(): Set<String> =
        context.dataStore.data.first()[Keys.ENABLED_BANKS] ?: emptySet()
}
