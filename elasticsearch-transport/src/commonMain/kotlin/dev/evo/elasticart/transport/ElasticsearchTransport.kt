package dev.evo.elasticart.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.request
import io.ktor.client.statement.readText
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.takeFrom

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import kotlinx.serialization.serializer

typealias BodyBuilder = StringBuilder.() -> Unit

sealed class ElasticsearchException(msg: String) : Exception(msg) {
    open class TransportError(
        val statusCode: Int,
        val error: String
    ) : ElasticsearchException("Elasticsearch server respond with an error") {
        private val json = Json(JsonConfiguration.Default)
        open val isRetriable = false

        companion object {
            private const val MAX_TEXT_ERROR_LENGTH = 80
        }

        fun reason(): String? {
            val reason = try {
                val info = json.parseJson(error)
                when (val error = info.jsonObject["error"]) {
                    null -> return null
                    is JsonObject -> {
                        val rootCause = error.getArray("root_cause").getObject(0)
                        StringBuilder().apply {
                            append(rootCause["reason"]?.content ?: return null)
                            rootCause["resource.id"]?.let(::append)
                            rootCause["resource.type"]?.let(::append)
                        }.toString()
                    }
                    else -> return error.content
                }

            } catch (ex: JsonException) {
                return error.slice(0 until error.length.coerceAtMost(MAX_TEXT_ERROR_LENGTH))
            } catch (ex: IllegalStateException) {
                return error.slice(0 until error.length.coerceAtMost(MAX_TEXT_ERROR_LENGTH))
            }
            return reason
        }

        override fun toString(): String {
            val reasonArg = when (val reason = reason()) {
                null -> ""
                else -> ", \"$reason\""
            }
            return "${this::class.simpleName}(${statusCode}${reasonArg})"
        }
    }
    class RequestError(error: String)
        : TransportError(400, error)
    class AuthenticationError(error: String)
        : TransportError(401, error)
    class AuthorizationError(error: String)
        : TransportError(403, error)
    class NotFoundError(error: String)
        : TransportError(404, error)
    class ConflictError(error: String)
        : TransportError(409, error)
    class GatewayTimeout(error: String)
        : TransportError(504, error)
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
        return processResponse(response)
    }

    private suspend fun processResponse(response: HttpResponse): String {
        val statusCode = response.status.value
        val content = response.readText()
        throw when (statusCode) {
            in 200..299 -> return content
            400 -> ElasticsearchException.RequestError(content)
            401 -> ElasticsearchException.AuthenticationError(content)
            403 -> ElasticsearchException.AuthorizationError(content)
            404 -> ElasticsearchException.NotFoundError(content)
            409 -> ElasticsearchException.ConflictError(content)
            504 -> ElasticsearchException.GatewayTimeout(content)
            else -> ElasticsearchException.TransportError(statusCode, content)
        }
    }
}
