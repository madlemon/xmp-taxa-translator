package io.github.madlemon.xmptaxatranslator.service

data class TranslationConfig(
    val iNaturalistKeyword: String = "Inat21",
    val preferredLocale: String = "de",
    val cacheFilePath: String? = null,
)
