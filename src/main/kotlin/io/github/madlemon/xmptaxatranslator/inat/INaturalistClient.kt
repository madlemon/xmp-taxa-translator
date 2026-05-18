package io.github.madlemon.xmptaxatranslator.inat

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class INaturalistClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun searchSpecies(query: String, preferredLocale: String): INatTaxon? {
        val response: INatResponse = client.get(
            "https://api.inaturalist.org/v1/taxa"
        ) {
            url {
                parameters.append("q", query)
                parameters.append("locale", preferredLocale)
                parameters.append("is_active", "true")
                parameters.append("rank", "species")
            }
        }.body()

        return response.results
            .firstOrNull { result ->
                normalize(result.matched_term) == normalize(query) ||
                        normalize(result.english_common_name) == normalize(query)
            }
    }

    fun normalize(text: String?): String? {
        return text?.trim()?.lowercase()
    }
}
