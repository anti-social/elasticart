package dev.evo.elasticart.transport

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ExperimentalStdlibApi
@io.ktor.util.KtorExperimentalAPI
class ElasticsearchKtorTransportTests {
    @Test
    fun testGetJsonRequest() = runTest {
        val client = ElasticsearchKtorTransport(Url("http://example.com:9200"), MockEngine { request ->
            assertEquals(
                request.method,
                HttpMethod.Get
            )
            assertEquals(
                "/_alias",
                request.url.encodedPath
            )
            respond(
                """
                {
                  "products_v1": {"aliases": {"products": {}}},
                  "orders_v2": {"aliases": {"orders": {}, "shopping_carts": {}}}
                }
                """.trimIndent(),
                headers = headersOf(
                    HttpHeaders.ContentType, ContentType.Application.Json.toString()
                )
            )
        })
        val aliases = client.jsonRequest(Method.GET, "_alias")
        assertEquals(setOf("products_v1", "orders_v2"), aliases.jsonObject.keys)
        assertEquals(
            JsonObject(emptyMap()),
            aliases.jsonObject
                .getObject("products_v1")
                .getObject("aliases")
                .getObject("products")
        )
    }

    @Test
    fun testPutJsonRequest() = runTest {
        val client = ElasticsearchKtorTransport(Url("http://example.com:9200"), MockEngine { request ->
            assertEquals(
                request.method,
                HttpMethod.Put
            )
            assertEquals(
                "/products/_settings",
                request.url.encodedPath
            )
            assertEquals(
                ContentType.Application.Json,
                request.body.contentType
            )
            assertEquals(
                """{"index":{"number_of_replicas":2}}""",
                request.body.toByteArray().decodeToString()
            )
            respond(
                """{"acknowledge": true}""",
                headers = headersOf(
                    HttpHeaders.ContentType, ContentType.Application.Json.toString()
                )
            )
        })
        val body = json {
            "index" to json {
                "number_of_replicas" to 2
            }
        }
        val result = client.jsonRequest(Method.PUT, "products/_settings", body = body)
        assertEquals(
            result,
            json {
                "acknowledge" to true
            }
        )
    }

    @Test
    fun testDeleteJsonRequest() = runTest {
        val client = ElasticsearchKtorTransport(Url("http://example.com:9200"), MockEngine { request ->
            assertEquals(
                request.method,
                HttpMethod.Delete
            )
            assertEquals(
                "/products_v2",
                request.url.encodedPath
            )
            assertEquals(
                "",
                request.body.toByteArray().decodeToString()
            )
            respond(
                """{"acknowledge": true}""",
                headers = headersOf(
                    HttpHeaders.ContentType, ContentType.Application.Json.toString()
                )
            )
        })
        val result = client.jsonRequest(Method.DELETE, "products_v2")
        assertEquals(
            result,
            json {
                "acknowledge" to true
            }
        )
    }

    @Test
    fun testPostJsonRequestWithTimeout() = runTest {
        val client = ElasticsearchKtorTransport(Url("http://example.com:9200"), MockEngine { request ->
            assertEquals(
                request.method,
                HttpMethod.Post
            )
            assertEquals(
                "/products_v2/_forcemerge",
                request.url.encodedPath
            )
            assertEquals(
                "",
                request.body.toByteArray().decodeToString()
            )
            respondError(
                HttpStatusCode.GatewayTimeout
            )
        })
        val ex = assertFailsWith(ElasticsearchException.GatewayTimeout::class) {
            client.jsonRequest(
                Method.POST, "products_v2/_forcemerge", mapOf("max_num_segments" to listOf("1"))
            )
        }
        assertEquals(504, ex.statusCode)
        assertEquals(
            "GatewayTimeout(504, \"Gateway Timeout\")",
            ex.toString()
        )
    }
}
