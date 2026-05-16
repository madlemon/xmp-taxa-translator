package io.github.madlemon.xmptaxatranslator.cache

import io.github.madlemon.xmptaxatranslator.model.TaxonTranslations
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FileTaxonTranslationCache(
    private val file: File
) : TaxonTranslationCache {

    private val lock = ReentrantLock()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
    }

    private var cache: MutableMap<String, TaxonTranslations?> = load()

    override suspend fun getOrPut(
        key: String,
        producer: suspend () -> TaxonTranslations?
    ): TaxonTranslations? {

        cache[key]?.let {
            return it
        }

        val value = producer()

        lock.withLock {
            cache[key] = value
            persist()
        }

        return value
    }

    private fun load(): MutableMap<String, TaxonTranslations?> {
        if (!file.exists()) return mutableMapOf()

        return try {
            val text = file.readText()
            json.decodeFromString<Map<String, TaxonTranslations?>>(text)
                .toMutableMap()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun persist() {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(cache))
    }
}
