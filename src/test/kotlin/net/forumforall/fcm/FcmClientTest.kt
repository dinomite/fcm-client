package net.forumforall.fcm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.apache.http.HttpHeaders
import org.apache.http.impl.client.HttpClients
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.URI
import java.util.concurrent.CompletionException

class FcmClientTest {
    val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
    val fcmUrl: URI
    val fcmKey = "The key"
    val objectMapper = ObjectMapper()

    val fcmClient: FcmClient
    val fcmUrlPathMatcher: UrlMatchingStrategy

    init {
        objectMapper.registerModule(KotlinModule())

        wireMockServer.start()
        fcmUrl = URI("http://localhost:${wireMockServer.port()}/firebase")
        fcmUrlPathMatcher = urlEqualTo(fcmUrl.path)

        fcmClient = FcmClientImpl(HttpClients.createDefault(), fcmUrl, fcmKey, objectMapper.writer(), objectMapper.reader())
    }

    @Test
    fun sendNotification() {
        val notification = FcmNotification(to = "device-token", notification = Notification(title = "title"))
        val expectedResponse = FcmResponse(7, 1, 0, 0, listOf())

        wireMockServer.stubFor(post(fcmUrlPathMatcher)
                .withRequestBody(equalTo(objectMapper.writeValueAsString(notification)))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.AUTHORIZATION, fcmKey)
                        .withStatus(200)
                        .withBody(objectMapper.writeValueAsString(expectedResponse))
                )
        )

        val future = fcmClient.sendNotification(notification)
        val fcmResponse = future.join()

        wireMockServer.verify(1, postRequestedFor(fcmUrlPathMatcher))
        assertEquals(expectedResponse, fcmResponse)
    }

    @Test
    fun sendNotification_HttpError() {
        val notification = FcmNotification(to = "device-token", notification = Notification(title = "title"))
        val expectedResponse = FcmResponse(7, 1, 0, 0, listOf())

        wireMockServer.stubFor(post(fcmUrlPathMatcher)
                .withRequestBody(equalTo(objectMapper.writeValueAsString(notification)))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.AUTHORIZATION, fcmKey)
                        .withStatus(401)
                        .withBody(objectMapper.writeValueAsString(expectedResponse))
                )
        )

        val future = fcmClient.sendNotification(notification)
        try {
            future.join()
            fail("Expected an exception")
        } catch (e: CompletionException) {
            val cause = e.cause!!
            assertEquals(cause.javaClass, FcmException::class.java)
            assertEquals(cause.message, "HTTP response from FCM: 401")
        }

        wireMockServer.verify(1, postRequestedFor(fcmUrlPathMatcher))
    }
}