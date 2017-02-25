package net.forumforall.fcm

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
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.function.Supplier
import kotlin.reflect.jvm.internal.impl.javax.inject.Inject

interface FcmClient {
    fun sendNotification(notification: FcmNotification): CompletableFuture<FcmResponse>
}

class FcmClientImpl
@Inject
constructor(val httpClient: CloseableHttpClient, val fcmUrl: URI, val fcmKey: String,
            val objectWriter: ObjectWriter, objectReader: ObjectReader,
            val executorService: ExecutorService? = null) : FcmClient {
    val responseReader: ObjectReader = objectReader.forType(FcmResponse::class.java)
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
            if (code != 200) {
                throw FcmException("HTTP response from FCM: " + code)
            }

            val entity = response.entity ?: throw FcmException("Empty entity from FCM")
            val entityString = EntityUtils.toString(entity, Charsets.UTF_8) ?: throw FcmException("Unable to decode response")

            return responseReader.readValue(entityString)
        }
    }
}
