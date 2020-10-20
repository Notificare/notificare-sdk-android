package re.notifica.internal.network.push.payloads

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareTransport

@JsonClass(generateAdapter = true)
internal data class NotificareDeviceRegistration(
    @Json(name = "deviceID") val deviceId: String,
    @Json(name = "oldDeviceID") val oldDeviceId: String?,
    @Json(name = "userID") val userId: String?,
    val userName: String?,
    val country: String?,
    val language: String,
    val region: String,
    val platform: String,
    val transport: NotificareTransport,
    val osVersion: String,
    val sdkVersion: String,
    val appVersion: String,
    val deviceString: String,
    val timeZoneOffset: Float,
    //val backgroundAppRefresh: Boolean
    val allowedUI: Boolean,
    val locationServicesAuthStatus: String,
    val bluetoothEnabled: Boolean,
)
