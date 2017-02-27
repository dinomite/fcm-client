package net.dinomite.fcm

import com.fasterxml.jackson.annotation.JsonCreator

data class FcmResponse(val multicastId: Long,
                       val success: Int,
                       val failure: Int,
                       val canonicalIds: Int,
                       val results: List<Result>)

data class Result(val messageId: String, val registrationId: String?, val error: Error?)

enum class Error(val text: String) {
    MISSING_REGISTRATION("error:MissingRegistration"),
    INVALID_REGISTRATION("error:InvalidRegistration"),
    NOT_REGISTERED("error:NotRegistered"),
    INVALID_PACKAGE_NAME("error:InvalidPackageName"),
    MISMATCH_SENDER_ID("error:MismatchSenderId"),
    INVALID_PARAMETERS("error:InvalidParameters"),
    MESSAGE_TOO_BIG("error:MessageTooBig"),
    INVALID_DATA_KEY("InvalidDataKey"),
    INVALID_TTL("error:InvalidTtl"),
    INTERNAL_SERVER_ERROR("error:InternalServerError"),
    DEVICE_MESSAGE_RATE_EXCEEDED("DeviceMessageRateExceeded"),
    TOPICS_MESSAGE_RATE_EXCEEDED("TopicsMessageRateExceeded"),
    INVALID_APNS_CREDENTIAL("InvalidApnsCredential");

    companion object {
        @JsonCreator
        fun fromString(search: String?): Error? {
            return Error.values().firstOrNull { it.text.equals(search, true) }
        }
    }
}