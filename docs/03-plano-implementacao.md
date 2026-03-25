# 3. Plano de Implementação em Etapas

---

## Etapa 1: Setup do Projeto

### Objetivo
Repositório Git com projeto Android compilando no Android Studio.

### O que fazer
1. Criar repositório `my-finance-notifier` no GitHub
2. Criar projeto Android Studio (Empty Compose Activity)
3. Configurar `build.gradle.kts` — min SDK 26, target SDK 34, Kotlin 2.0+
4. Configurar version catalog (`gradle/libs.versions.toml`) com todas as dependências
5. Configurar Hilt: `@HiltAndroidApp` no Application, `@AndroidEntryPoint` no MainActivity
6. `.gitignore` para Android (build/, .gradle/, local.properties, *.apk)

### Entregável
- Projeto compila via `./gradlew assembleDebug`
- App abre tela em branco no dispositivo/emulador

---

## Etapa 2: Camada de Dados Local

### Objetivo
Room + DataStore configurados e testados.

### O que fazer
1. Criar enum `BankApp` com `packageName`, `bancoKey` e `displayName` para cada banco
2. Criar `NotificationLogEntity` com anotações Room (`@Entity`, `@PrimaryKey`)
3. Criar `NotificationLogDao` com queries: `getAll()`, `getFailed()`, `insert()`, `updateStatus()`, `deleteOlderThan()`
4. Criar `AppDatabase` estendendo `RoomDatabase`
5. Criar `SettingsDataStore` wrapper sobre `Preferences DataStore`
6. Criar `SettingsRepository` expondo settings como `Flow`
7. Testes unitários para `NotificationLogDao`

### Entregável
- Dados persistem localmente entre reinicializações
- Testes unitários passando

---

## Etapa 3: Camada de Dados Remota

### Objetivo
Retrofit configurado e capaz de enviar payloads ao webhook.

### O que fazer
1. Criar `WebhookPayload` data class com campos `banco`, `texto`, `dataHora`
2. Criar `WebhookApi` interface Retrofit com método `sendNotification()`
3. Criar `NetworkModule` (Hilt) fornecendo OkHttp com logging interceptor + Retrofit
4. Criar `NotificationRepository` com lógica de: save → send → update status → enqueue retry
5. Testes unitários com mock do Retrofit (MockWebServer)

### Entregável
- Chamada HTTP funcional ao backend
- Testes passando

---

## Etapa 4: NotificationListenerService

### Objetivo
Capturar notificações de apps bancários automaticamente.

### O que fazer
1. Declarar `NotificationCaptureService` no `AndroidManifest.xml`:
   - Permissão `BIND_NOTIFICATION_LISTENER_SERVICE`
   - Intent filter `android.service.notification.NotificationListenerService`
2. Implementar `onNotificationPosted()`:
   - Filtrar por packageName dos bancos habilitados
   - Extrair texto via `notification.extras.getString(Notification.EXTRA_TEXT)`
   - Mapear packageName → `banco` via `BankApp.fromPackageName()`
   - Montar `WebhookPayload` e delegar ao `NotificationRepository`
3. Implementar deduplicação com cache em memória (janela de 5 segundos)
4. Criar notification channel + foreground notification para confiabilidade em OEMs agressivas
5. **Testar em dispositivo físico** — emulador não suporta NotificationListenerService adequadamente

### Pontos de atenção
- O usuário precisa conceder permissão manualmente em Configurações → Notificações
- Em Xiaomi/Samsung/Huawei, pode ser necessário desabilitar otimização de bateria para o app

### Entregável
- Notificação de banco capturada e enviada ao backend automaticamente

---

## Etapa 5: RetryWorker

### Objetivo
Garantir entrega de notificações quando houver falha de rede.

### O que fazer
1. Implementar `RetryWorker` estendendo `CoroutineWorker`
2. Buscar logs `FAILED` no Room e retentar envio
3. Configurar constraints: `NetworkType.CONNECTED`
4. Configurar backoff exponencial: 30s inicial, máximo 5 minutos
5. Enfileirar automaticamente em falhas no `NotificationRepository`
6. Implementar worker periódico de cleanup: deletar logs com mais de 30 dias

### Entregável
- Falhas de envio são retentadas automaticamente quando a internet retorna
- Logs antigos são limpos periodicamente

---

## Etapa 6: UI — Tela de Configurações

### Objetivo
Tela para o usuário configurar webhook e bancos monitorados.

### O que fazer
1. Implementar `MainViewModel` com `StateFlow` para settings e lista de logs
2. Implementar `SettingsScreen` com Jetpack Compose:
   - Campo de texto para URL base
   - Campo de texto para token UUID
   - Checkboxes para cada banco (Nubank, Bradesco, C6)
   - Indicador de status do serviço (ativo/inativo)
   - Botão "Testar conexão" que envia payload de teste
   - Botão para abrir `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`
3. Implementar `PermissionScreen` para onboarding
4. Persistir alterações via `SettingsRepository`

### Entregável
- Usuário configura webhook URL, token e seleciona bancos pela interface

---

## Etapa 7: UI — Tela de Histórico

### Objetivo
Visualizar notificações enviadas, pendentes e falhadas.

### O que fazer
1. Implementar `LogScreen` com `LazyColumn` observando Room via `Flow`
2. Cada item mostra: banco, preview do texto (80 chars), timestamp, chip de status
3. Chips coloridos: verde (`SENT`), vermelho (`FAILED`), amarelo (`PENDING`)
4. Botão "Retentar falhos" que enfileira `RetryWorker` manualmente
5. Botão "Limpar histórico" com diálogo de confirmação

### Entregável
- Usuário visualiza status de cada notificação enviada em tempo real

---

## Etapa 8: Navegação e Polimento

### Objetivo
App completo, navegável e com tratamento de edge cases.

### O que fazer
1. Configurar `AppNavigation` com bottom navigation: Configurações e Histórico
2. Implementar fluxo de primeiro uso: exibir `PermissionScreen` se acesso a notificações não concedido
3. Validações: URL vazia, nenhum banco selecionado, formato de URL inválido
4. Feedback visual: `Snackbar` para resultado do teste de conexão
5. Ícone do app e tema Material 3 com cores do My Finance

### Entregável
- App navegável, completo e com UX polida

---

## Etapa 9: Documentação

### Objetivo
Documentação completa no padrão do projeto My Finance.

### O que fazer
1. `docs/01-arquitetura.md` — diagramas, fluxo de dados, componentes
2. `docs/02-componentes.md` — detalhamento técnico de cada módulo
3. `docs/03-plano-implementacao.md` — este documento
4. `docs/04-decisoes-tecnicas.md` — justificativas tecnológicas
5. `README.md` — setup rápido e instruções de instalação

### Entregável
- Documentação completa em português, mesma estrutura do projeto My Finance

---

## Etapa 10: Build e Distribuição

### Objetivo
APK instalável no dispositivo do usuário.

### O que fazer
1. Gerar keystore para release: `keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000`
2. Configurar signing no `app/build.gradle.kts` via `local.properties` (nunca commitado)
3. Build release com R8/ProGuard habilitado
4. Gerar APK ou criar GitHub Release para download
5. Documentar processo de instalação (habilitar fontes desconhecidas, instalar APK)

### Entregável
- APK pronto para sideload no dispositivo
