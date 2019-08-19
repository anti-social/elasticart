package dev.evo.elasticart.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.serialization.json.JsonObject

import kotlin.test.Test
import kotlin.test.assertEquals

class ElasticsearchClientTests {
    @Test
    fun test_getAliases() = runTest {
        val client = ElasticsearchClient(Url("http://example.com:9200"), MockEngine { request ->
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
        val aliases = client.getAliases()
        assertEquals(setOf("products_v1", "orders_v2"), aliases.keys)
        assertEquals(JsonObject(emptyMap()), aliases["products_v1"]!!.aliases["products"])
    }
}
