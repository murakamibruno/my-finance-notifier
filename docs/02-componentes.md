# 2. Componentes Técnicos

---

## 2.1 NotificationCaptureService

Estende `android.service.notification.NotificationListenerService`, uma API do Android que permite ao app receber callbacks quando qualquer notificação é postada ou removida no dispositivo.

### Permissão necessária

O usuário precisa conceder manualmente o acesso em **Configurações → Notificações → Acesso a notificações**. Não é uma permissão runtime padrão — requer navegação explícita nas configurações do sistema.

### Ciclo de vida

- **`onListenerConnected()`** — chamado quando o Android vincula o serviço. Indica que o serviço está ativo.
- **`onNotificationPosted(sbn: StatusBarNotification)`** — chamado a cada nova notificação no dispositivo.
- **`onListenerDisconnected()`** — chamado quando o Android desvincula o serviço.

### Lógica de captura

```kotlin
override fun onNotificationPosted(sbn: StatusBarNotification) {
    val bankApp = BankApp.fromPackageName(sbn.packageName) ?: return
    if (!settingsDataStore.isBankEnabled(bankApp)) return

    val texto = sbn.notification.extras.getString(Notification.EXTRA_TEXT) ?: return
    if (isDuplicate(sbn.packageName, texto, sbn.postTime)) return

    val payload = WebhookPayload(
        banco = bankApp.bancoKey,
        texto = texto,
        dataHora = sbn.postTime
    )

    coroutineScope.launch {
        notificationRepository.send(payload)
    }
}
```

### Deduplicação

Mantém um cache em memória com hash de `(packageName + texto + timestamp)`. Se a mesma combinação aparecer dentro de 5 segundos, ignora. Isso previne que notificações duplicadas do Android gerem transações duplicadas.

### Foreground Notification

Em OEMs agressivas (Xiaomi, Samsung, Huawei), o sistema pode matar serviços em background. Uma notificação persistente ("My Finance Notifier está monitorando") melhora a confiabilidade e dá ao usuário feedback visual de que o serviço está ativo.

---

## 2.2 NotificationRepository

Orquestra o fluxo completo:

1. **Salva localmente** — `INSERT` no Room com status `PENDING`
2. **Tenta enviar** — `POST` via Retrofit
3. **Atualiza status**:
   - `201` → `SENT`
   - Qualquer erro → `FAILED` + incrementa `retryCount`
4. **Agenda retry** — enfileira `RetryWorker` via WorkManager se falhou

```kotlin
suspend fun send(payload: WebhookPayload) {
    val logId = dao.insert(payload.toEntity(status = "PENDING"))

    try {
        val response = webhookApi.sendNotification(settings.token, payload)
        if (response.isSuccessful) {
            dao.updateStatus(logId, "SENT", response.code())
        } else {
            dao.updateStatus(logId, "FAILED", response.code())
            enqueueRetry()
        }
    } catch (e: Exception) {
        dao.updateStatus(logId, "FAILED", null)
        enqueueRetry()
    }
}
```

---

## 2.3 RetryWorker (WorkManager)

`CoroutineWorker` que busca registros `FAILED` no Room e retenta o envio.

### Configuração

| Parâmetro | Valor |
|---|---|
| Constraint | `NetworkType.CONNECTED` |
| Backoff inicial | 30 segundos |
| Backoff máximo | 5 minutos |
| Estratégia | Exponencial |

### Cleanup

Um worker periódico (daily) deleta logs com mais de 30 dias para evitar crescimento infinito do SQLite.

---

## 2.4 Room Database

### Tabela `notification_log`

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | Long (PK, auto) | Identificador único |
| `banco` | String | Chave do banco (`nubank`, `bradesco`, `c6`) |
| `texto` | String | Texto bruto da notificação (max 500 chars) |
| `dataHora` | Long | Timestamp da notificação (epoch millis) |
| `status` | String | `PENDING`, `SENT`, `FAILED` |
| `httpStatus` | Int? | Código HTTP da resposta (null se erro de rede) |
| `createdAt` | Long | Timestamp de inserção no Room |
| `retryCount` | Int | Número de tentativas de reenvio |

### Queries principais

| Operação | Descrição |
|---|---|
| `getAll(): Flow<List<Entity>>` | Todos os logs (para UI reativa) |
| `getFailed(): List<Entity>` | Logs com status FAILED (para retry) |
| `insert(entity)` | Insere novo log |
| `updateStatus(id, status, httpStatus)` | Atualiza status após envio |
| `deleteOlderThan(timestamp)` | Cleanup de logs antigos |

---

## 2.5 SettingsDataStore

Usa `Preferences DataStore` para persistir configurações:

| Chave | Tipo | Descrição | Default |
|---|---|---|---|
| `webhook_base_url` | String | URL base do backend | `""` |
| `webhook_token` | String | UUID do token | `""` |
| `enabled_banks` | Set<String> | Bancos habilitados | `emptySet()` |

---

## 2.6 WebhookApi (Retrofit)

```kotlin
interface WebhookApi {
    @POST("api/webhook/{token}")
    suspend fun sendNotification(
        @Path("token") token: String,
        @Body payload: WebhookPayload
    ): Response<Any>
}
```

### WebhookPayload

```kotlin
data class WebhookPayload(
    val banco: String,
    val texto: String,
    val dataHora: Long
)
```

---

## 2.7 BankApp Enum

```kotlin
enum class BankApp(
    val packageName: String,
    val bancoKey: String,
    val displayName: String
) {
    NUBANK("com.nu.production", "nubank", "Nubank"),
    BRADESCO("com.bradesco", "bradesco", "Bradesco"),
    C6("com.c6bank.app", "c6", "C6 Bank");

    companion object {
        fun fromPackageName(pkg: String): BankApp? =
            entries.find { it.packageName == pkg }
    }
}
```
