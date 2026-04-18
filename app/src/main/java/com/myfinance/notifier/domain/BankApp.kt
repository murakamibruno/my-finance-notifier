package com.myfinance.notifier.domain

enum class SourceType { NOTIFICATION, SMS }

enum class BankApp(
    val packageName: String,
    val bancoKey: String,
    val displayName: String,
    val sourceType: SourceType = SourceType.NOTIFICATION,
    val senderAddress: String? = null
) {
    NUBANK("com.nu.production", "nubank", "Nubank"),
    BRADESCO("com.bradesco", "bradesco", "Bradesco"),
    BRADESCO_CARTOES("br.com.bradesco.cartoes", "bradesco", "Bradesco Cartões"),
    C6("com.c6bank.app", "c6", "C6 Bank"),
    BANCO_DO_BRASIL("br.com.bb.android", "banco_do_brasil", "Banco do Brasil"),
    INTER("br.com.intermedium", "inter", "Inter"),
    ALELO("air.br.com.alelo.mobile.android", "alelo", "Alelo"),
    BRADESCO_SMS("", "bradesco", "Bradesco (SMS)", SourceType.SMS, "27398");

    companion object {
        fun fromPackageName(pkg: String): BankApp? =
            entries.find { it.sourceType == SourceType.NOTIFICATION && it.packageName == pkg }

        fun fromSenderAddress(address: String): BankApp? =
            entries.find { it.sourceType == SourceType.SMS && it.senderAddress == address }
    }
}
