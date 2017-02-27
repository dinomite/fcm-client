package net.dinomite.fcm

import org.junit.Assert.assertEquals
import org.junit.Test

class FcmResponseTest : DataClassTest() {
    val result = Result("foo", "bar", Error.NOT_REGISTERED)
    val response = FcmResponse(1234567891011, 7, 3, 9, listOf(Result("foobar", "bazqux", Error.MISSING_REGISTRATION)))

    @Test
    fun serializeResult() {
        assertEquals(fixture("result.json"), objectMapper.writeValueAsString(result))
    }

    @Test
    fun deserializeResult() {
        assertEquals(result, objectMapper.readValue(fixture("result.json"), Result::class.java))
    }

    @Test
    fun serializeFcmResponse() {
        assertEquals(fixture("response.json"), objectMapper.writeValueAsString(response))
    }

    @Test
    fun deserializeFcmResponse() {
        assertEquals(response, objectMapper.readValue(fixture("response.json"), FcmResponse::class.java))
    }
}

