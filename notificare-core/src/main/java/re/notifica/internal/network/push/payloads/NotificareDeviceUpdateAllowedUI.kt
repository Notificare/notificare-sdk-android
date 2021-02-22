package re.notifica.internal.network.push.payloads

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class NotificareDeviceUpdateAllowedUI(
    val language: String,
    val region: String,
    val allowedUI: Boolean,
)
