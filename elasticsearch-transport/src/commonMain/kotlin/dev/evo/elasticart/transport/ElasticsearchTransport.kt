@file:Suppress("NAME_SHADOWING")

package dev.evo.elasticart.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.compression.ContentEncoding
import io.ktor.client.request.request
import io.ktor.client.statement.readText
import io.ktor.client.statement.HttpResponse
import io.ktor.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.takeFrom
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

sealed class ElasticsearchException(msg: String) : Exception(msg) {
    open class TransportError(
        val statusCode: Int,
        val error: String
    ) : ElasticsearchException("Elasticsearch server respond with an error") {
        private val json = Json.Default
        open val isRetriable = false

        companion object {
            private const val MAX_TEXT_ERROR_LENGTH = 80

            private fun jsonElementToString(element: JsonElement) = when (element) {
                is JsonObject -> element.toString()
                is JsonArray -> element.toString()
                is JsonPrimitive -> element.content
            }

            private fun trimRawError(error: String) =
                error.slice(0 until error.length.coerceAtMost(MAX_TEXT_ERROR_LENGTH))
        }

        fun reason(): String? {
            return try {
                val info = json.decodeFromString(JsonElement.serializer(), error).jsonObject
                when (val error = info["error"]) {
                    null -> return null
                    is JsonObject -> {
                        val rootCause = error["root_cause"]?.jsonArray?.get(0)?.jsonObject
                            ?: return null
                        StringBuilder().apply {
                            append(
                                jsonElementToString(rootCause["reason"] ?: return null)
                            )
                            rootCause["resource.id"]?.let(::append)
                            rootCause["resource.type"]?.let(::append)
                        }.toString()
                    }
                    is JsonPrimitive -> error.content
                    is JsonArray -> error.toString()
                }

            } catch (ex: SerializationException) {
                return trimRawError(error)
            } catch (ex: IllegalStateException) {
                return trimRawError(error)
            }
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

interface RequestEncoderFactory {
    val encoding: String?
    fun create(): RequestEncoder
}

interface RequestEncoder : Appendable {
    override fun append(value: Char): Appendable {
        TODO("not implemented")
    }
    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable {
        TODO("not implemented")
    }
    fun toByteArray(): ByteArray
}

class StringEncoderFactory : RequestEncoderFactory {
    override val encoding: String? = null
    override fun create() = StringEncoder()
}

class StringEncoder : RequestEncoder {
    private val builder = StringBuilder()

    override fun append(value: CharSequence?): Appendable {
        return builder.append(value)
    }

    override fun toByteArray(): ByteArray {
        return builder.toString().toByteArray(Charsets.UTF_8)
    }
}

class GzipEncoderFactory : RequestEncoderFactory {
    override val encoding = "gzip"
    override fun create() = GzipEncoder()
}
expect class GzipEncoder() : RequestEncoder

typealias RequestBodyBuilder = RequestEncoder.() -> Unit


abstract class ElasticsearchTransport(
    val baseUrl: String,
    config: Config,
) {
    companion object {
        private val json = Json.Default
    }

    class Config {
        var gzipRequests: Boolean = false
    }

    protected val requestEncoderFactory: RequestEncoderFactory = if (config.gzipRequests) {
        GzipEncoderFactory()
    } else {
        StringEncoderFactory()
    }

    suspend fun jsonRequest(
        method: Method,
        path: String,
        parameters: Map<String, List<String>>? = null,
        body: JsonElement? = null
    ): JsonElement {
        val response = if (body != null) {
            request(method, path, parameters) {
                append(json.encodeToString(JsonElement.serializer(), body))
            }
        } else {
            request(method, path, parameters, null)
        }
        return json.decodeFromString(JsonElement.serializer(), response)
    }

    abstract suspend fun request(
        method: Method,
        path: String,
        parameters: Map<String, List<String>>? = null,
        contentType: String? = null,
        bodyBuilder: RequestBodyBuilder? = null
    ): String
}

class ElasticsearchKtorTransport(
    baseUrl: String,
    engine: HttpClientEngine,
    configure: Config.() -> Unit = {},
) : ElasticsearchTransport(
    baseUrl,
    Config().apply(configure),
) {

    private val client = HttpClient(engine) {
        expectSuccess = false

        // Enable compressed response from Elasticsearch
        ContentEncoding()
    }

    override suspend fun request(
        method: Method,
        path: String,
        parameters: Map<String, List<String>>?,
        contentType: String?,
        bodyBuilder: RequestBodyBuilder?
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
                val requestEncoder = requestEncoderFactory.create().apply(bodyBuilder)
                requestEncoderFactory.encoding?.let { encoding ->
                    this.headers[HttpHeaders.ContentEncoding] = encoding
                }

                val contentType = if (contentType != null) {
                    val (contentType, contentSubType) = contentType.split('/', limit = 2)
                    ContentType(contentType, contentSubType)
                } else {
                    ContentType.Application.Json
                }
                this.body = ByteArrayContent(
                    requestEncoder.toByteArray(),
                    contentType
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
