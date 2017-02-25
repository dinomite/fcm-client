package net.forumforall.fcm

data class FcmResponse(val multicastId: Long,
                       val success: Int,
                       val failure: Int,
                       val canonicalIds: Int,
                       val results: List<Result>)

data class Result(val messageId: String, val registrationId: String?, val error: String?)