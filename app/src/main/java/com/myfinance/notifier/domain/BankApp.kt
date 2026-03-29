package com.myfinance.notifier.domain

enum class BankApp(
    val packageName: String,
    val bancoKey: String,
    val displayName: String
) {
    NUBANK("com.nu.production", "nubank", "Nubank"),
    BRADESCO("com.bradesco", "bradesco", "Bradesco"),
    BRADESCO_CARTOES("br.com.bradesco.cartoes", "bradesco", "Bradesco Cartões"),
    C6("com.c6bank.app", "c6", "C6 Bank"),
    BANCO_DO_BRASIL("br.com.bb.android", "banco_do_brasil", "Banco do Brasil"),
    INTER("br.com.intermedium", "inter", "Inter"),
    ALELO("br.com.alelo.carteiradigital", "alelo", "Alelo");

    companion object {
        fun fromPackageName(pkg: String): BankApp? =
            entries.find { it.packageName == pkg }
    }
}
