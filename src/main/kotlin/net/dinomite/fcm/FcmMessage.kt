package net.dinomite.fcm

data class FcmNotification(val to: String,
                           val registrationIds: List<String>? = null,
                           val condition: String? = null,
                           val collapseKey: String? = null,
                           val priority: String? = null,
                           val timeToLive: Int? = null,
                           val delayWhileIdle: Boolean? = null,
                           val restrictedPackageName: String? = null,
                           val notification: Notification? = null,
                           val data: Map<String, String>? = null)

data class Notification(val title: String,
                        val body: String? = null,
                        val icon: String? = null,
                        val sound: String? = null,
                        val tag: String? = null,
                        val color: String? = null,
                        val clickAction: String? = null,
                        val bodyLocKey: String? = null,
                        val bodyLocArgs: String? = null,
                        val titleLocKey: String? = null,
                        val titleLocArgs: String? = null)
