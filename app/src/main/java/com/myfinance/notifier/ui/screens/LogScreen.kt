package com.myfinance.notifier.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myfinance.notifier.domain.BankApp
import com.myfinance.notifier.domain.NotificationLog
import com.myfinance.notifier.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val retryingIds by viewModel.retryingIds.collectAsState()
    val retryingAll by viewModel.retryingAll.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val hasFailed = logs.any { it.status == NotificationLog.STATUS_FAILED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Histórico",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { viewModel.retryFailed() },
                enabled = hasFailed && !retryingAll,
                modifier = Modifier.weight(1f)
            ) {
                if (retryingAll) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Retentar todos")
                }
            }
            OutlinedButton(
                onClick = { showClearDialog = true },
                enabled = logs.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Limpar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Text(
                text = "Nenhuma notificação registrada",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItem(
                        log = log,
                        isRetrying = log.id in retryingIds,
                        onRetry = { viewModel.retrySingle(log.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpar histórico") },
            text = { Text("Tem certeza que deseja apagar todo o histórico?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearLogs()
                    showClearDialog = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun LogItem(log: NotificationLog, isRetrying: Boolean, onRetry: () -> Unit) {
    val statusColor = when {
        isRetrying -> Color(0xFFF59E0B)
        log.status == NotificationLog.STATUS_SENT -> Color(0xFF10B981)
        log.status == NotificationLog.STATUS_FAILED -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    val statusText = when {
        isRetrying -> "Retentando..."
        log.status == NotificationLog.STATUS_SENT -> "Enviado"
        log.status == NotificationLog.STATUS_FAILED -> "Falhou"
        else -> "Pendente"
    }

    val bankName = BankApp.entries
        .find { it.bancoKey == log.banco }?.displayName ?: log.banco

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bankName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isRetrying) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = statusColor,
                            strokeWidth = 2.dp
                        )
                    } else if (log.status == NotificationLog.STATUS_FAILED) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retentar",
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.texto,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(log.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (log.retryCount > 0) {
                        Text(
                            text = "${log.retryCount}x retry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (log.httpStatus != null) {
                        Text(
                            text = "HTTP ${log.httpStatus}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
