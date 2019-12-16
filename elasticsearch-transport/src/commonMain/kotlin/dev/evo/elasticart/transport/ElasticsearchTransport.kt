package dev.evo.elasticart.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.request
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.*
import io.ktor.http.content.TextContent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

typealias BodyBuilder = StringBuilder.() -> Unit

sealed class ElasticsearchException(msg: String) : Exception(msg) {
    open class Transport(
        val statusCode: Int,
        val statusDescription: String,
        msg: String = "Elasticsearch server respond with an error"
    ) : ElasticsearchException(msg) {
        open val isRetriable = false
    }
    class Request(statusDescription: String)
        : Transport(400, statusDescription)
    class Authentication(statusDescription: String)
        : Transport(401, statusDescription)
    class Authorization(statusDescription: String)
        : Transport(403, statusDescription)
    class NotFound(statusDescription: String)
        : Transport(404, statusDescription)
    class Conflict(statusDescription: String)
        : Transport(409, statusDescription)
    class GatewayTimeout(statusDescription: String)
        : Transport(504, statusDescription)
}

enum class Method {
    GET, PUT, POST, DELETE
}

interface ElasticsearchTransport {
    companion object {
        internal val json = Json(JsonConfiguration.Default)
    }

    suspend fun jsonRequest(
        method: Method, path: String, parameters: Map<String, List<String>>? = null, body: JsonElement? = null
    ): JsonElement {
        val response = if (body != null) {
            request(method, path, parameters) {
                append(json.stringify(JsonElement::class.serializer(), body))
            }
        } else {
            request(method, path, parameters, null)
        }
        return json.parseJson(response)
    }

    suspend fun request(
        method: Method, path: String,
        parameters: Map<String, List<String>>? = null,
        bodyBuilder: BodyBuilder? = null
    ): String
}

class ElasticsearchKtorTransport(
    private val baseUrl: Url, engine: HttpClientEngine
) : ElasticsearchTransport {

    private val client = HttpClient(engine) {
        expectSuccess = false
    }

    override suspend fun request(
        method: Method,
        path: String,
        parameters: Map<String, List<String>>?,
        bodyBuilder: BodyBuilder?
    ): String {
        val ktorHttpMethod = when (method) {
            Method.GET -> HttpMethod.Get
            Method.PUT -> HttpMethod.Put
            Method.POST -> HttpMethod.Post
            Method.DELETE -> HttpMethod.Delete
        }
        val ktorParameters = if (parameters != null) {
            Parameters.build {
                parameters.forEach { (name, values) ->
                    appendAll(name, values)
                }
            }
        } else {
            Parameters.Empty
        }

        val response = client.request<HttpResponse> {
            this.method = ktorHttpMethod
            url {
                takeFrom(baseUrl)
                this.path(path)
                if (parameters != null) {
                    this.parameters.appendAll(ktorParameters)
                }
            }
            if (bodyBuilder != null) {
                this.body = TextContent(
                    StringBuilder().apply(bodyBuilder).toString(),
                    ContentType.Application.Json
                )
            }
        }
        when (response.status.value) {
            400 -> throw ElasticsearchException.Request(response.status.description)
            401 -> throw ElasticsearchException.Authentication(response.status.description)
            403 -> throw ElasticsearchException.Authorization(response.status.description)
            404 -> throw ElasticsearchException.NotFound(response.status.description)
            409 -> throw ElasticsearchException.Conflict(response.status.description)
            504 -> throw ElasticsearchException.GatewayTimeout(response.status.description)
            in 400..599 -> throw ElasticsearchException.Transport(response.status.value, response.status.description)
        }
        return response.readText()
    }
}
