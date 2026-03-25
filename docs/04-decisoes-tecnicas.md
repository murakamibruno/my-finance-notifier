# 4. Decisões Técnicas

---

## 4.1 Por que Kotlin e não Java?

Kotlin é a linguagem oficial recomendada pelo Google para desenvolvimento Android desde 2019. Oferece:
- **Null safety** — reduz NullPointerException em tempo de compilação
- **Coroutines** — concorrência estruturada para operações assíncronas (HTTP, Room)
- **Data classes** — DTOs sem boilerplate
- **Extension functions** — código mais expressivo
- **100% interop com Java** — pode usar qualquer biblioteca Java existente

---

## 4.2 Por que Jetpack Compose e não XML Views?

Compose é o toolkit moderno de UI declarativa do Android:
- **Menos código** — sem `findViewById`, adapters ou XML layouts
- **Reativo** — UI atualiza automaticamente ao mudar o estado (`StateFlow`)
- **Preview** — visualização em tempo real no Android Studio
- **Material 3** — componentes prontos com design system atualizado

Para um app com 3 telas simples, Compose é significativamente mais produtivo que XML.

---

## 4.3 Por que Room e não SQLite direto?

Room é uma abstração sobre SQLite que oferece:
- **Type safety** — queries verificadas em tempo de compilação
- **Flow/LiveData** — queries reativas para a UI
- **Migrations** — versionamento do schema
- **Menos boilerplate** — DAOs com anotações ao invés de cursors manuais

Usamos Room apenas para o histórico de notificações — não é um banco de dados crítico. Se o usuário limpar os dados do app, nenhuma informação importante é perdida (tudo que importa está no PostgreSQL do backend).

---

## 4.4 Por que DataStore e não SharedPreferences?

DataStore é o substituto moderno do SharedPreferences:
- **Coroutine-safe** — operações não bloqueiam a main thread
- **Flow** — leitura reativa
- **Consistency** — transações atômicas
- **Não usa `apply()` que pode perder dados** — garante persistência

Para configurações simples (URL, token, set de bancos), DataStore Preferences é o ideal.

---

## 4.5 Por que WorkManager e não AlarmManager/custom retry?

WorkManager é a solução recomendada pelo Google para trabalho assíncrono confiável:
- **Sobrevive a app kills** — Android re-agenda o trabalho
- **Sobrevive a reboots** — com `RECEIVE_BOOT_COMPLETED`
- **Constraints** — executa apenas quando há rede (`NetworkType.CONNECTED`)
- **Backoff exponencial** — nativo, sem implementação manual
- **Integra com Hilt** — injeção de dependências nos workers

Para retry de envios falhados, é a escolha natural.

---

## 4.6 Por que Hilt e não Koin ou DI manual?

Hilt é a solução oficial de DI do Google para Android, construída sobre Dagger:
- **Integração nativa** com ViewModel, WorkManager, Navigation
- **Compile-time validation** — erros de DI detectados na compilação
- **Geração de código** — performance superior em runtime
- **Escopo automático** — gerencia ciclo de vida dos objetos

Para um app com Retrofit, Room, DataStore e WorkManager, Hilt simplifica significativamente a inicialização e injeção.

---

## 4.7 NotificationListenerService vs AccessibilityService

Existem duas APIs do Android para capturar informações de outros apps:

| Aspecto | NotificationListenerService | AccessibilityService |
|---|---|---|
| Propósito | Escutar notificações | Assistência a deficientes |
| Permissão | Acesso a notificações | Acessibilidade |
| Complexidade | Simples | Complexa |
| Restrições Play Store | Nenhuma | Severas (pode causar remoção) |
| Confiabilidade | Alta | Alta, mas mais invasiva |

**Decisão:** `NotificationListenerService` é a escolha correta. Captura exatamente o que precisamos (texto da notificação) sem acessar nada além disso.

---

## 4.8 Foreground Notification para confiabilidade

OEMs como Xiaomi, Samsung e Huawei possuem otimizações agressivas de bateria que podem matar serviços em background. Uma notificação persistente ("My Finance Notifier está monitorando") sinaliza ao sistema que o serviço é importante.

Referência: [dontkillmyapp.com](https://dontkillmyapp.com) documenta as particularidades de cada fabricante.

---

## 4.9 Deduplicação de notificações

O Android pode postar a mesma notificação múltiplas vezes (atualização de conteúdo, re-post pelo sistema). Para evitar transações duplicadas no backend, o app mantém um cache em memória com hash de `(packageName + texto + timestamp)` e ignora duplicatas dentro de uma janela de 5 segundos.

Essa abordagem foi escolhida ao invés de deduplicação no backend porque:
- Economiza chamadas HTTP desnecessárias
- O backend não tem contexto suficiente para identificar duplicatas de notificação (o texto bruto pode ser idêntico para transações diferentes)

---

## 4.10 Min SDK 26 (Android 8.0)

A escolha de SDK 26 como mínimo se baseia em:
- `NotificationListenerService` é mais confiável a partir do Android 8.0
- Notification Channels (obrigatórios para foreground notification) foram introduzidos no SDK 26
- Cobre **~95%** dos dispositivos Android ativos (dados de 2024)
- Permite usar APIs modernas sem fallbacks excessivos

---

## 4.11 Parsing no backend, não no app

O app envia o texto **bruto** da notificação. Todo o parsing (regex, extração de valor, estabelecimento, tipo) acontece no backend.

**Razões:**
- **Centralizado** — uma única base de código com os parsers
- **Atualizável** — mudança no formato da notificação requer apenas deploy no backend
- **Testável** — parsers já cobertos por testes unitários no projeto My Finance
- **App simples** — o app Android faz apenas captura + envio, sem lógica de negócio
