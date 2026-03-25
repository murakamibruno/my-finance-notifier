package com.myfinance.notifier.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.myfinance.notifier.domain.BankApp
import com.myfinance.notifier.service.NotificationCaptureService
import com.myfinance.notifier.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val webhookUrl by viewModel.webhookUrl.collectAsState()
    val enabledBanks by viewModel.enabledBanks.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val context = LocalContext.current

    var serviceEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var batteryOptimized by remember { mutableStateOf(isBatteryOptimized(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        serviceEnabled = isNotificationListenerEnabled(context)
        batteryOptimized = isBatteryOptimized(context)
    }

    var urlInput by remember(webhookUrl) { mutableStateOf(webhookUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineMedium
        )

        // Status do serviço
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (serviceEnabled)
                    Color(0xFF10B981).copy(alpha = 0.1f)
                else
                    Color(0xFFEF4444).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (serviceEnabled)
                        Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (serviceEnabled)
                        Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (serviceEnabled)
                        "Serviço ativo" else "Serviço inativo",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Webhook URL
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("URL do webhook") },
            placeholder = { Text("https://dominio.com/api/webhook/token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Salvar
        Button(
            onClick = { viewModel.saveWebhookUrl(urlInput) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bancos
        Text(
            text = "Bancos monitorados",
            style = MaterialTheme.typography.titleMedium
        )

        BankApp.entries.forEach { bank ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = bank.name in enabledBanks,
                    onCheckedChange = { checked ->
                        viewModel.toggleBank(bank.name, checked)
                    }
                )
                Text(text = bank.displayName)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Testar conexão
        Button(
            onClick = { viewModel.testConnection() },
            modifier = Modifier.fillMaxWidth()
        ) {
            when (testResult) {
                is MainViewModel.TestResult.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                else -> Text("Testar conexão")
            }
        }

        testResult?.let { result ->
            when (result) {
                is MainViewModel.TestResult.Success -> {
                    Text(
                        text = "Conexão bem-sucedida!",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is MainViewModel.TestResult.Error -> {
                    Text(
                        text = "Erro: ${result.message}",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {}
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Abrir configurações de notificação
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Habilitar acesso a notificações")
        }

        // Otimização de bateria
        if (batteryOptimized) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Otimização de bateria ativa",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "O Android pode encerrar o serviço em segundo plano. Desabilite a otimização de bateria para este app.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Desabilitar otimização de bateria")
                    }
                }
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val cn = ComponentName(context, NotificationCaptureService::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(cn.flattenToString()) == true
}

private fun isBatteryOptimized(context: android.content.Context): Boolean {
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    return !pm.isIgnoringBatteryOptimizations(context.packageName)
}
