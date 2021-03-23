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
    val timeZoneOffset: Float,
    val backgroundAppRefresh: Boolean,
    val allowedUI: Boolean,
    val locationServicesAuthStatus: String,
    val bluetoothEnabled: Boolean,
)

@JsonClass(generateAdapter = true)
internal data class DeviceUpdateNotificationSettingsPayload(
    val language: String,
    val region: String,
    val allowedUI: Boolean,
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
    val timeZoneOffset: Float,
)

@JsonClass(generateAdapter = true)
internal data class DeviceTagsPayload(
    val tags: List<String>,
)
