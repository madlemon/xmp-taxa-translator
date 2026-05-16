package io.github.madlemon.xmptaxatranslator

import io.github.madlemon.xmptaxatranslator.service.TranslationConfig
import io.github.madlemon.xmptaxatranslator.service.TranslationService

const val PREFERRED_LOCALE = "de"

const val I_NATURALIST_KEYWORD = "Inat21"

fun main(args: Array<String>) {
    println("Hello XMP Taxa Translator")

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info")

    val path = "C:\\Users\\klocke\\Desktop\\lumina-docker\\photos"

    val service = TranslationService(
        TranslationConfig(preferredLocale = "de")
    )
    service.processDirectory(path)

    println("DONE!")
}


