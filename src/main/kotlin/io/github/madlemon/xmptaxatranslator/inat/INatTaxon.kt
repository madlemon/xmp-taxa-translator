package io.github.madlemon.xmptaxatranslator.inat

import kotlinx.serialization.Serializable

@Serializable
data class INatTaxon(
    val id: Int,
    val name: String, // Latin name
    val preferred_common_name: String? = null,
    val rank: String? = null
)
