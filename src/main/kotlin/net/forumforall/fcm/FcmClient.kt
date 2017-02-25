package net.forumforall.fcm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import org.apache.commons.codec.Charsets
import org.apache.http.Header
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

interface FcmClient {
    fun sendNotification(notification: FcmNotification): CompletableFuture<FcmResponse>
}

/**
 *
 */
class FcmClientImpl(val httpClient: CloseableHttpClient, val fcmUrl: URI, fcmKey: String,
            objectMapper: ObjectMapper, val executorService: ExecutorService? = null) : FcmClient {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    val responseReader: ObjectReader = objectMapper.readerFor(FcmResponse::class.java)
    val objectWriter: ObjectWriter = objectMapper.writer()
    val contentType: ContentType = ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8)
    val authHeader: Header = BasicHeader(HttpHeaders.AUTHORIZATION, "key=$fcmKey")

    override fun sendNotification(notification: FcmNotification): CompletableFuture<FcmResponse> {
        if (executorService == null) {
            return CompletableFuture.supplyAsync { send(notification) }
        } else {
            return CompletableFuture.supplyAsync(Supplier { send(notification) }, executorService)
        }
    }

    private fun send(notification: FcmNotification): FcmResponse {
        val post = HttpPost(fcmUrl)
        post.entity = StringEntity(objectWriter.writeValueAsString(notification), contentType)
        post.addHeader(authHeader)

        httpClient.execute(post).use { response ->
            val code = response.statusLine.statusCode
            if (code == 401) throw FcmException("Authentication Error")
            if (code != 200) throw FcmException("Unhappy HTTP response from FCM: " + code)

            val entity = response.entity ?: throw FcmException("Empty entity from FCM")
            val entityString = EntityUtils.toString(entity, Charsets.UTF_8) ?: throw FcmException("Unable to decode response")

            return responseReader.readValue(entityString)
        }
    }
}
