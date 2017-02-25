# fcm-client

A simple client for the [Firebase Cloud Messaging (FCM) HTTP API](https://firebase.google.com/docs/cloud-messaging/http-server-ref).
FCM is the replacement for Google Cloud Messaging (GCM) for sending push notifications (PuNo)
to Android devices.

Firebase doesn't provide a Java library for sending messages, and simply points you to the
[HTTP protocl docs](https://firebase.google.com/docs/cloud-messaging/http-server-ref).  This
library is a simple implementation of that doc including objects to represent the messages you
send to Firebase and the responses they give.

# Usage

First, construct an `FcmClient`:

```kotlin
FcmClientImpl(HttpClients.createDefault(),
              "https://fcm.googleapis.com/fcm/send",
              "your-fcm-server-key",
              ObjectMapper())
```

There is an optional final parameter to the `FcmClientImpl`, an `ExecutorService` to use when
sending notifications.  If none is provided, the `ForkJoinPool` is used.

Sending messages:

```kotlin
val notification = FcmNotification(to = "device-token",
                                   notification = Notification(title = "title"))

val future = fcmClient.sendNotification(fcmNotification)
```

`sendNotification()` gives you a [`CompletableFuture`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html).
If FCM rejects a message with 

```kotlin
future.whenComplete { fcmResponse, throwable ->
    if (throwable != null) {
        logger.warn("Error sending to FCM", throwable)
    } else if (fcmResponse.success == 1) {
        logger.info("Push notification accepted by FCM")
    } else {
        logger.warn("Notification rejected by FCM: $fcmResponse")

        fcmResponse.results.forEach {
            if (it.error == NOT_REGISTERED) {
                logger.info("$NOT_REGISTERED for token $token (registrationId ${it.registrationId})")
                tokenDao.deleteToken(token)
            } else if (it.error != null) {
                logger.warn("Error from FCM for registrationId ${it.registrationId}: ${it.error}")
            }
        }
    }
}
```
