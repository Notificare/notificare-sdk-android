package re.notifica.internal.network.push.payloads

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class NotificareDeviceUpdateTimeZone(
    val language: String,
    val region: String,
    val timeZoneOffset: Float,
)
