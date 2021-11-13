package re.notifica.internal.network.push

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareTransport

@JsonClass(generateAdapter = true)
internal data class DeviceRegistrationPayload(
    @Json(name = "deviceID") val deviceId: String,
    @Json(name = "oldDeviceID") val oldDeviceId: String?,
    @Json(name = "userID") val userId: String?,
    val userName: String?,
    val language: String,
    val region: String,
    val platform: String,
    val transport: NotificareTransport,
    val osVersion: String,
    val sdkVersion: String,
    val appVersion: String,
    val deviceString: String,
    val timeZoneOffset: Double,
    val backgroundAppRefresh: Boolean,
)

@JsonClass(generateAdapter = true)
internal data class DeviceUpdateLanguagePayload(
    val language: String,
    val region: String,
)

@JsonClass(generateAdapter = true)
internal data class DeviceUpdateTimeZonePayload(
    val language: String,
    val region: String,
    val timeZoneOffset: Double,
)

@JsonClass(generateAdapter = true)
internal data class DeviceTagsPayload(
    val tags: List<String>,
)

@JsonClass(generateAdapter = true)
internal data class CreateNotificationReplyPayload(
    @Json(name = "notification") val notificationId: String,
    @Json(name = "deviceID") val deviceId: String,
    @Json(name = "userID") val userId: String?,
    val label: String,
    val data: Data,
) {

    @JsonClass(generateAdapter = true)
    internal data class Data(
        val target: String?,
        val message: String?,
        val media: String?,
        val mimeType: String?,
    )
}

@JsonClass(generateAdapter = true)
internal data class TestDeviceRegistrationPayload(
    @Json(name = "deviceID") val deviceId: String,
)
