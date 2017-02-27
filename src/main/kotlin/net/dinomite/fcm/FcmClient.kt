package net.dinomite.fcm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
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

/**
 * A client for sending messages to Firebase Cloud messaging.
 *
 * @param fcmUrl            The URL to communicate with FCM (probably https://fcm.googleapis.com/fcm/send)
 * @param fcmKey            The Server key (from your project in the <a href="https://console.firebase.google.com">Firebase Console</a>)
 * @param httpClient        A <a href="http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/client/CloseableHttpClient.html">CloseableHttpClient</a>
 * @param objectMapper      An <a href="https://fasterxml.github.io/jackson-databind/javadoc/2.8/com/fasterxml/jackson/databind/ObjectMapper.html">ObjectMapper</a>, used for (de)serialization of messages when communicating with FCM
 * @param executorService   An ExecutorService used for dispatching requests.  If none is provided, <a href="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html">ForkJoinPool is used</a>
 *
 * @returns An FcmClient
 */
open class FcmClient(val fcmUrl: URI, fcmKey: String, val httpClient: CloseableHttpClient,
                    objectMapper: ObjectMapper, val executorService: ExecutorService? = null) {
    val responseReader: ObjectReader = objectMapper.readerFor(FcmResponse::class.java)
    val objectWriter: ObjectWriter = objectMapper.writer()
    val contentType: ContentType = ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8)
    val authHeader: Header = BasicHeader(HttpHeaders.AUTHORIZATION, "key=$fcmKey")

    /**
     * Send a notification to Firebase
     *
     * @param message   An {@link net.dinomite.fcm.FcmMessage} to send to Firebase
     *
     * @returns A CompletableFuture<FcmResponse>, which throws FcmException for fatal errors
     */
    fun sendNotification(message: FcmMessage): CompletableFuture<FcmResponse> {
        if (executorService == null) {
            return CompletableFuture.supplyAsync { send(message) }
        } else {
            return CompletableFuture.supplyAsync(Supplier { send(message) }, executorService)
        }
    }

    private fun send(message: FcmMessage): FcmResponse {
        val post = HttpPost(fcmUrl)
        post.entity = StringEntity(objectWriter.writeValueAsString(message), contentType)
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
