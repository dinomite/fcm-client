# fcm-client

A simple client for the [Firebase Cloud Messaging (FCM) HTTP API](https://firebase.google.com/docs/cloud-messaging/http-server-ref).
FCM is the replacement for Google Cloud Messaging (GCM) for sending push notifications (PuNo)
to Android devices.

Firebase doesn't provide a Java library for sending messages, and simply points you to the
[HTTP protocl docs](https://firebase.google.com/docs/cloud-messaging/http-server-ref).  This
library is a simple implementation of that doc including objects to represent the messages you
send to Firebase and the responses they give.

# Usage (in [Kotlin](https://blog.plan99.net/why-kotlin-is-my-next-programming-language-c25c001e26e3#.bmukwp6az))

First, construct an `FcmClient`:

```kotlin
FcmClient(URI("https://fcm.googleapis.com/fcm/send"), "your-fcm-server-key",
              HttpClients.createDefault(), ObjectMapper())
```

There is an optional final parameter to the `FcmClient`, an `ExecutorService` to use when
sending notifications.  If none is provided, the `ForkJoinPool` is used.

Sending messages:

```kotlin
val token = "device-token"
val message = FcmMessage(to = token, notification = Notification(title = "title"))
val future = fcmClient.sendNotification(message)
```

`sendNotification()` gives you a [`CompletableFuture`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html).
The future will complete exceptionally if FCM responds with an authentication or HTTP error,
or if `FcmClient` was unable to read & parse the response.

If the message was submitted successfully, the response from FCM contains a number of things
mentioned in the documentation, but the important parts as I understand it are removing tokens
from your database that FCM has said are not registered (that is, the user unregistered their
device) and updating tokens that FCM says have changed.

For handling the response from sending to a single device, I use this handler:

```kotlin
future.whenComplete { fcmResponse, throwable ->
    if (throwable != null) {
        logger.warn("Error sending to FCM", throwable)
    } else if (fcmResponse.failure == 1) {
        logger.warn("Notification rejected by FCM: $fcmResponse")

        fcmResponse.results.forEach {
            if (it.error == Error.NOT_REGISTERED) {
                logger.info("${Error.NOT_REGISTERED} for token <$token> (registrationId ${it.registrationId})")
                // Delete token from your DB
            } else if (it.error != null) {
                logger.warn("Error from FCM for registrationId ${it.registrationId}: ${it.error}")
            }
        }
    } else {
        if (fcmResponse.success == 1) {
            logger.info("Push notification accepted by FCM")
        }

        if (fcmResponse.canonicalIds == 1) {
            val newToken = fcmResponse.results.first().registrationId
            if (newToken != null) {
                // Update the token in your DB
            }
        }
    }
}
```

Handling responses to multicast messages is left as an exercise for the reader.

# Usage (in Java)

The above examples are written in Kotlin; below is equivalent code in Java to make it easier
to understand for those who don't know the newer (and better, Kotlin).
You'll note the lack of [builders](https://en.wikipedia.org/wiki/Builder_pattern) for the complex
FCM data holding objects.  Kotlin has named arguments, which means you don't need to write builders
and that object construction is less verbose.  Unfortunately, this means that when using those
objects in Java you have to supply all fields, even optional ones.  The best fix is to start using
Kotlin, which is incredibly easy to add to any Java project and will make your life better.

```java
FcmClient fcmClient = new FcmClient(
        new URI("https://fcm.googleapis.com/fcm/send"),
        "your-fcm-server-key",
        HttpClients.createDefault(),
        new ObjectMapper(),
        null);

String token = "device-token";
Notification notification = new Notification("title", null, null, null, null, null, null, null, null, null, null);
FcmMessage message = new FcmMessage(token, null, null, null, null, null, null, null, notification, null);
CompletableFuture<FcmResponse> future = fcmClient.sendNotification(message);

future.whenComplete((fcmResponse, throwable) -> {
    if (throwable != null) {
        logger.warn("Error sending to FCM", throwable);
    } else if (fcmResponse.getFailure() == 1) {
        logger.warn("Notification rejected by FCM: $fcmResponse");

        List<Result> results = fcmResponse.getResults();
        for (Result result : results) {
            if (result.getError() == Error.NOT_REGISTERED) {
                logger.info(Error.NOT_REGISTERED + " for token <" + token +
                        "> (registrationId " + result.getRegistrationId() + ")");
                // Delete token from your DB
            } else if (result.getError() != null) {
                logger.warn("Error from FCM for registrationId " + result.getRegistrationId() + ": " + result.getError());
            }
        }
    } else {
        if (fcmResponse.getSuccess() == 1) {
            logger.info("Push notification accepted by FCM");
        }

        if (fcmResponse.getCanonicalIds() == 1) {
            String newToken = fcmResponse.getResults().get(1).getRegistrationId();
            if (newToken != null) {
                // Update the token in your DB
            }
        }
    }
});
```
