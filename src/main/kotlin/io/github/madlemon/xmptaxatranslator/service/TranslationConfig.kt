package io.github.madlemon.xmptaxatranslator.service

data class TranslationConfig(
    val iNaturalistModel: String = "Inat21",
    val preferredLocale: String = "de",
    val cacheFilePath: String? = null,
    val xmpDirectoryPath: String,
)
