package com.myfinance.notifier.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.myfinance.notifier.domain.BankApp
import com.myfinance.notifier.domain.SourceType
import com.myfinance.notifier.service.NotificationCaptureService
import com.myfinance.notifier.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val webhookUrl by viewModel.webhookUrl.collectAsState()
    val enabledBanks by viewModel.enabledBanks.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val testBank by viewModel.testBank.collectAsState()
    val urlSaved by viewModel.urlSaved.collectAsState()

    LaunchedEffect(urlSaved) {
        if (urlSaved) {
            delay(2000)
            viewModel.clearUrlSaved()
        }
    }
    val context = LocalContext.current

    var serviceEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var batteryOptimized by remember { mutableStateOf(isBatteryOptimized(context)) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleBank(BankApp.BRADESCO_SMS.name, true)
        }
    }

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
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
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
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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
            if (urlSaved) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text("Salvar")
            }
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
                        if (bank.sourceType == SourceType.SMS && checked) {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECEIVE_SMS
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.toggleBank(bank.name, true)
                            } else {
                                smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                            }
                        } else {
                            viewModel.toggleBank(bank.name, checked)
                        }
                    }
                )
                Text(text = bank.displayName)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Testar conexão
        Text(
            text = "Testar conexão",
            style = MaterialTheme.typography.titleMedium
        )

        var bankDropdownExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = bankDropdownExpanded,
                onExpandedChange = { bankDropdownExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = testBank.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Banco") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = bankDropdownExpanded,
                    onDismissRequest = { bankDropdownExpanded = false }
                ) {
                    BankApp.entries.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank.displayName) },
                            onClick = {
                                viewModel.setTestBank(bank)
                                bankDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.testConnection() },
                enabled = testResult !is MainViewModel.TestResult.Loading
            ) {
                if (testResult is MainViewModel.TestResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Testar")
                }
            }
        }

        testResult?.let { result ->
            when (result) {
                is MainViewModel.TestResult.Success -> {
                    Text(
                        text = "Conexão bem-sucedida!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is MainViewModel.TestResult.Error -> {
                    Text(
                        text = "Erro: ${result.message}",
                        color = MaterialTheme.colorScheme.error,
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
                    containerColor = Color(0xFFF5C842).copy(alpha = 0.1f)
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
                            tint = Color(0xFFF5C842),
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
