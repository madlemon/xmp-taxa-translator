package io.github.madlemon.xmptaxatranslator.model

import kotlinx.serialization.Serializable

@Serializable
data class TaxonTranslations (
    val englishCommonName: String,
    val preferredCommonName: String,
    val latinName: String?
)
