package dev.evo.elasticart.transport

import kotlin.test.Test
import kotlin.test.assertEquals

class ElasticsearchExceptionTests {
    @Test
    fun testParsingJsonErrorWithReason() {
        val ex = ElasticsearchException.TransportError(
            500,
            """{"error": {"root_cause": [{"type": "error", "reason": "error reason"}]}}"""
        )
        assertEquals(
            "TransportError(500, \"error reason\")",
            ex.toString()
        )
    }

    @Test
    fun testParsingJsonError() {
        val ex = ElasticsearchException.TransportError(
            500,
            """{"error": "something error message"}"""
        )
        assertEquals(
            "TransportError(500, \"something error message\")",
            ex.toString()
        )
    }
    @Test
    fun testParsingTextError() {
        val ex = ElasticsearchException.TransportError(
            500,
            """text error message"""
        )
        assertEquals(
            "TransportError(500, \"text error message\")",
            ex.toString()
        )
    }
}