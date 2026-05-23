package io.github.madlemon.xmptaxatranslator

import io.github.madlemon.xmptaxatranslator.service.TranslationConfig
import io.github.madlemon.xmptaxatranslator.service.TranslationService

fun main(args: Array<String>) {
    println("[START] XMP Taxa Translator")

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info")

    val argMap = args
        .toList()
        .chunked(2)
        .associate { it[0] to it[1] }


    val service = TranslationService(
        TranslationConfig(
            preferredLocale = argMap["--locale"] ?: "de",
            xmpDirectoryPath = argMap["--xmp-dir"]
                ?: error("Missing required argument: --xmp-dir"),
            cacheFilePath = argMap["--cache-file"]
        )
    )
    service.processDirectory()

    println("[DONE] Translation completed")
}


