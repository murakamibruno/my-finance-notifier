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
- JDK 21

## Setup

1. Clone o repositório
2. Abra no Android Studio
3. Sync Gradle
4. Build & Run no dispositivo

## Instalação (APK)

### Via GitHub Releases

1. Acesse a página de [Releases](https://github.com/murakamibruno/my-finance-notifier/releases)
2. Baixe o APK da versão mais recente
3. Transfira para o dispositivo (ou baixe direto pelo celular)
4. Habilite "Instalar de fontes desconhecidas"
5. Instale e abra o app
6. Configure URL do webhook e token
7. Selecione os bancos
8. Conceda acesso a notificações em Configurações do Android

### Build local

1. Configure o signing em `local.properties`:
   ```properties
   RELEASE_STORE_FILE=../release.keystore
   RELEASE_STORE_PASSWORD=<senha>
   RELEASE_KEY_ALIAS=release
   RELEASE_KEY_PASSWORD=<senha>
   ```
2. Gere o APK: `./gradlew assembleRelease`
3. O APK estará em `app/build/outputs/apk/release/`

## CI/CD

O projeto usa GitHub Actions para gerar e publicar o APK automaticamente.

Para criar um release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

O workflow faz o build do APK assinado e publica na página de [Releases](https://github.com/murakamibruno/my-finance-notifier/releases).

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
