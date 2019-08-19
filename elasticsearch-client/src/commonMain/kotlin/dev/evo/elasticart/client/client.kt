package dev.evo.elasticart.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.http.takeFrom
import io.ktor.http.Url

import kotlinx.io.core.Closeable

open class ElasticsearchClientException(
    val statusCode: Int, val statusDescription: String,
    message: String = "Elasticsearch server respond with an error"
) : Exception("$message: $statusCode $statusDescription")
{
    open val isRetriable = false
}

class ElasticsearchClient(
    val baseUrl: Url,
    httpClientEngine: HttpClientEngine
) : Closeable {
    private val httpClient = HttpClient(httpClientEngine) {
        // expectSuccess = false
        install(JsonFeature) {
            serializer = KotlinxSerializer().apply {
                register(Aliases.serializer())
            }
        }
    }

    companion object {
        private const val ALIAS_ENDPOINT = "_alias"
    }

    suspend fun getAliases(index: String? = null, alias: String? = null): Aliases {
        val pathComponents = pathWithIndex(ALIAS_ENDPOINT, index)
        if (!alias.isNullOrEmpty()) {
            pathComponents.add(alias)
        }
        return httpClient.get {
            url.takeFrom(baseUrl).path(pathComponents)
        }
    }

    private fun pathWithIndex(apiPath: String, index: String?): MutableList<String> {
        return if (!index.isNullOrEmpty()) {
            mutableListOf(index, apiPath)
        } else {
            mutableListOf(apiPath)
        }
    }

    override fun close() {
        httpClient.close()
    }
}
