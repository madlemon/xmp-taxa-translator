package io.github.madlemon.xmptaxatranslator.inat

import kotlinx.serialization.Serializable

@Serializable
data class INatResponse(
    val results: List<INatTaxon>
)
