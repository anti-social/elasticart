package dev.evo.elasticart.client

import kotlin.test.Test

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.MissingFieldException

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@kotlinx.serialization.UnstableDefault
class ModelTests {
    val json = Json(JsonConfiguration.Default)

    @Test
    fun testGetAliases() {
        val aliasesResponse = """
        {
          "logs_20162801" : {
            "aliases" : {
              "2016" : {
                "filter" : {
                  "term" : {
                    "year" : 2016
                  }
                }
              }
            }
          }
        }
        """.trimIndent()
        json.parse(Aliases.serializer(), aliasesResponse)
    }
}
