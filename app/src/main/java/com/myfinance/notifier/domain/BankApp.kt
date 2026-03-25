package com.myfinance.notifier.domain

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
