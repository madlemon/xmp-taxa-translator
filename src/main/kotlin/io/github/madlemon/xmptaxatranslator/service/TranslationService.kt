package io.github.madlemon.xmptaxatranslator.service

import io.github.madlemon.xmptaxatranslator.cache.FileTaxonTranslationCache
import io.github.madlemon.xmptaxatranslator.cache.InMemoryTaxonTranslationCache
import io.github.madlemon.xmptaxatranslator.cache.TaxonTranslationCache
import io.github.madlemon.xmptaxatranslator.inat.INaturalistClient
import io.github.madlemon.xmptaxatranslator.model.TaxonTranslations
import io.github.madlemon.xmptaxatranslator.model.XmpTaxonMetadata
import io.github.madlemon.xmptaxatranslator.xmp.DarktableXmpWriter
import io.github.madlemon.xmptaxatranslator.xmp.LuminaXmpWriter
import io.github.madlemon.xmptaxatranslator.xmp.XmpReader
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
            config.iNaturalistModel,
            config.cacheFilePath
        )

        cache = if (config.cacheFilePath != null) {
            FileTaxonTranslationCache(File(config.cacheFilePath))
        } else {
            InMemoryTaxonTranslationCache()
        }

        val xmpFiles = dir
            .walkTopDown()
            .filter { file ->
                file.isFile &&
                        file.extension.equals("xmp", ignoreCase = true) &&
                        !isDarktableSidecar(file)
            }
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

    private fun isDarktableSidecar(file: File): Boolean {
        val name = file.nameWithoutExtension // removes .xmp

        return name.contains(".") // implies raw extension exists
    }


    private fun process(luminarXmpPath: String) = runBlocking {
        logger.info("Processing $luminarXmpPath")
        logger.info("Reading XMP...")
        val reader = XmpReader(config.iNaturalistModel)
        val metadata = reader.read(luminarXmpPath)

        logger.info("Description: ${metadata.detectedSpecies}")
        logger.info("Keywords:")
        metadata.keywords.forEach {
            logger.info(" - $it")
        }

        val englishName = metadata.detectedSpecies
            ?: run {
                logger.info("No description found, skipping iNaturalist lookup.")
                return@runBlocking
            }

        if (!metadata.keywords.contains(config.iNaturalistModel)) {
            logger.warn("No iNat21 classification found, skipping iNaturalist lookup.")
            return@runBlocking
        }


        val translation = cache.getOrPut(normalize(englishName)) {
            logger.info("Fetching translations from iNaturalist API...")

            rateLimit()
            val result = iNat.searchSpecies(englishName, config.preferredLocale)

            result?.let {
                TaxonTranslations(
                    englishCommonName = englishName,
                    preferredCommonName = it.preferred_common_name.orEmpty(),
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

        val darkTableXmp = findDarktableSidecar(luminarXmpPath)
        val darktableXmpExists = darkTableXmp != null
        if (darktableXmpExists) {
            logger.info("Adding classification info to existing Darktable XMP...")
            updateDarktableXmp(metadata, translation, darkTableXmp)
        }
        logger.info("Adding translation to Lumina XMP...")
        updateLuminaXmp(luminarXmpPath, metadata, translation)


    }

    private fun updateLuminaXmp(
        luminarXmpPath: String,
        metadata: XmpTaxonMetadata,
        translation: TaxonTranslations
    ) {
        val writer = LuminaXmpWriter(config.preferredLocale)
        writer.write(luminarXmpPath, metadata, translation)
    }

    private fun updateDarktableXmp(
        metadata: XmpTaxonMetadata,
        translation: TaxonTranslations,
        darkTableXmp: File
    ) {
        val darktableXmpWriter = DarktableXmpWriter()
        val keywords = (metadata.keywords
                - translation.englishCommonName
                + translation.preferredCommonName)
            .toMutableSet()
            .toList()
            .joinToString("|")
        darktableXmpWriter.write(darkTableXmp.path, translation, keywords)
    }

    fun findDarktableSidecar(luminarXmpPath: String): File? {
        val luminarFile = File(luminarXmpPath)
        val dir = luminarFile.parentFile ?: return null

        val baseName = luminarFile.nameWithoutExtension

        return dir.listFiles()
            ?.firstOrNull { file ->
                file.name.startsWith("$baseName.") &&
                        file.name.endsWith(".xmp") &&
                        file.name != luminarFile.name
            }
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
