package io.github.madlemon.xmptaxatranslator.cache

import io.github.madlemon.xmptaxatranslator.model.TaxonTranslations

class InMemoryTaxonTranslationCache : TaxonTranslationCache {

    private val cache = mutableMapOf<String, TaxonTranslations?>()

    override suspend fun getOrPut(
        key: String,
        producer: suspend () -> TaxonTranslations?
    ): TaxonTranslations? {
        if (cache.containsKey(key)) {
            return cache[key]
        }

        val value = producer()

        cache[key] = value

        return value
    }


}
