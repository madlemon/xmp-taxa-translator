package io.github.madlemon.xmptaxatranslator.cache

import io.github.madlemon.xmptaxatranslator.model.TaxonTranslations

interface TaxonTranslationCache {

    suspend fun getOrPut(
        key: String, producer: suspend () -> TaxonTranslations?
    ): TaxonTranslations?
}
