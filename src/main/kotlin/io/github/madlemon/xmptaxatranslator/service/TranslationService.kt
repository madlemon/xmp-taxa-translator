package io.github.madlemon.xmptaxatranslator.service

import io.github.madlemon.xmptaxatranslator.inat.INaturalistClient
import io.github.madlemon.xmptaxatranslator.model.TaxonTranslations
import io.github.madlemon.xmptaxatranslator.xmp.XmpReader
import io.github.madlemon.xmptaxatranslator.xmp.XmpWriter
import kotlinx.coroutines.runBlocking
import java.io.File

class TranslationService(
    private val config: TranslationConfig = TranslationConfig()
) {

    private val logger = org.slf4j.LoggerFactory.getLogger(TranslationService::class.java)

    private val requestDelayMs = 1200L
    private var lastRequestTime = 0L

    fun processDirectory(directoryPath: String) = runBlocking {
        val dir = File(directoryPath)

        if (!dir.exists() || !dir.isDirectory) {
            logger.error("Invalid directory: $directoryPath")
            return@runBlocking
        }

        logger.info("Config: locale={}, keyword={}", config.preferredLocale, config.iNaturalistKeyword)

        val xmpFiles = dir
            .walkTopDown()
            .filter { it.isFile && it.extension.lowercase() == "xmp" }
            .toList()

        logger.info("Found ${xmpFiles.size} XMP files")

        xmpFiles.forEach { file ->
            try {
                process(file.absolutePath)
            } catch (e: Exception) {
                logger.error("Failed processing ${file.name}: ${e.message}")
            }
        }
    }

    private fun process(filePath: String) = runBlocking {
        logger.info("Processing $filePath")
        logger.info("Reading XMP...")
        val reader = XmpReader()
        val metadata = reader.read(filePath)

        logger.info("Description: ${metadata.description}")
        logger.info("Keywords:")
        metadata.iptcKeywords.forEach {
            logger.info(" - $it")
        }

        val description = metadata.description
            ?: run {
                logger.info("No description found, skipping iNaturalist lookup.")
                return@runBlocking
            }


        if (!metadata.iptcKeywords.contains(config.iNaturalistKeyword)) {
            logger.warn("No iNat21 keyword found, skipping iNaturalist lookup.")
            return@runBlocking
        }

        logger.info("Fetching translations from iNaturalist API...")
        val iNat = INaturalistClient()
        rateLimit()
        val result = iNat.searchSpecies(description, config.preferredLocale)

        logger.info("Latin: ${result?.name}")
        logger.info("${config.preferredLocale}: ${result?.preferred_common_name}")

        val translation = TaxonTranslations(
            metadata.description,
            result?.preferred_common_name,
            result?.name
        )

        logger.info("Adding new Keywords to XMP...")
        val writer = XmpWriter()
        writer.write(filePath, metadata, translation)
    }

    private suspend fun rateLimit() {
        val now = System.currentTimeMillis()
        val wait = requestDelayMs - (now - lastRequestTime)

        if (wait > 0) {
            kotlinx.coroutines.delay(wait)
        }

        lastRequestTime = System.currentTimeMillis()
    }

}
