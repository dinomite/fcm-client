package net.dinomite.fcm

import org.junit.Assert.assertEquals
import org.junit.Test

class FcmMessageTest : DataClassTest() {
    val notification = Notification("title", "body", "icon", "sound", "tag", "color", "clickAction", "bodyLocKey",
            "bodyLocArgs", "titleLocKey", "titleLocArgs")
    val message = FcmMessage("to", listOf("foo", "bar"), "condition", "collapseKey", "priority", 7, false,
            "restrictedPackageName", notification, mapOf(Pair("foo", "bar")))

    @Test
    fun serializeNotification() {
        assertEquals(fixture("notification.json"), objectMapper.writeValueAsString(notification))
    }

    @Test
    fun deserializeNotification() {
        assertEquals(notification, objectMapper.readValue(fixture("notification.json"), Notification::class.java))
    }

    @Test
    fun serializeMessage() {
        assertEquals(fixture("message.json"), objectMapper.writeValueAsString(message))
    }

    @Test
    fun deserializeMessage() {
        assertEquals(message, objectMapper.readValue(fixture("message.json"), FcmMessage::class.java))
    }
}

