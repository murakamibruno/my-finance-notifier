# My Finance Notifier

App Android nativo que captura notificações bancárias e envia para o backend [My Finance](https://github.com/murakamibruno/my-finance-java) via webhook.

Substitui o MacroDroid com uma solução dedicada — basta configurar a URL do webhook e selecionar os bancos a monitorar.

## Bancos suportados

| Banco | Package |
|---|---|
| Nubank | `com.nu.production` |
| Bradesco | `com.bradesco` |
| C6 Bank | `com.c6bank.app` |

## Requisitos

- Android 8.0+ (API 26)
- Android Studio Ladybug (2024.2+)
- JDK 17

## Setup

1. Clone o repositório
2. Abra no Android Studio
3. Sync Gradle
4. Build & Run no dispositivo

## Instalação (APK)

1. Gere o APK: `./gradlew assembleRelease`
2. Transfira para o dispositivo
3. Habilite "Instalar de fontes desconhecidas"
4. Instale e abra o app
5. Configure URL do webhook e token
6. Selecione os bancos
7. Conceda acesso a notificações em Configurações do Android

## Documentação

- [Arquitetura](docs/01-arquitetura.md)
- [Componentes](docs/02-componentes.md)
- [Plano de Implementação](docs/03-plano-implementacao.md)
- [Decisões Técnicas](docs/04-decisoes-tecnicas.md)

## Stack

- **Kotlin** + Jetpack Compose
- **Retrofit** + OkHttp (HTTP)
- **Room** (SQLite local)
- **DataStore** (preferências)
- **WorkManager** (retry)
- **Hilt** (DI)
