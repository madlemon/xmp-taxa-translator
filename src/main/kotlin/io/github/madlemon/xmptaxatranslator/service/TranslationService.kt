package io.github.madlemon.xmptaxatranslator.service

import io.github.madlemon.xmptaxatranslator.cache.FileTaxonTranslationCache
import io.github.madlemon.xmptaxatranslator.cache.InMemoryTaxonTranslationCache
import io.github.madlemon.xmptaxatranslator.cache.TaxonTranslationCache
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

    private lateinit var cache: TaxonTranslationCache
    private val iNat = INaturalistClient()

    fun processDirectory(directoryPath: String) = runBlocking {
        val dir = File(directoryPath)

        if (!dir.exists() || !dir.isDirectory) {
            logger.error("Invalid directory: $directoryPath")
            return@runBlocking
        }

        logger.info(
            "Config: locale={}, keyword={}, cacheFilePath={}",
            config.preferredLocale,
            config.iNaturalistKeyword,
            config.cacheFilePath
        )

        cache = if (config.cacheFilePath != null) {
            FileTaxonTranslationCache(File(config.cacheFilePath))
        } else {
            InMemoryTaxonTranslationCache()
        }

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

        val englishName = metadata.description
            ?: run {
                logger.info("No description found, skipping iNaturalist lookup.")
                return@runBlocking
            }

        if (!metadata.iptcKeywords.contains(config.iNaturalistKeyword)) {
            logger.warn("No iNat21 keyword found, skipping iNaturalist lookup.")
            return@runBlocking
        }


        val translation = cache.getOrPut(normalize(englishName)) {
            logger.info("Fetching translations from iNaturalist API...")

            rateLimit()
            val result = iNat.searchSpecies(englishName, config.preferredLocale)

            result?.let {
                TaxonTranslations(
                    englishCommonName = englishName,
                    preferredCommonName = it.preferred_common_name,
                    latinName = it.name
                )
            }
        }

        if (translation == null) {
            logger.warn("No translation found for {}", englishName)
            return@runBlocking
        }

        logger.info("Latin: ${translation.latinName}")
        logger.info("${config.preferredLocale}: ${translation.preferredCommonName}")

        logger.info("Adding new Keywords to XMP...")
        val writer = XmpWriter()
        writer.write(filePath, metadata, translation)
    }

    private fun normalize(query: String): String {
        return query.trim().lowercase()
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
