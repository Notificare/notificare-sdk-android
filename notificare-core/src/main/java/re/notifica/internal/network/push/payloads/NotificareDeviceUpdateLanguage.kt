package re.notifica.internal.network.push.payloads

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class NotificareDeviceUpdateLanguage (
    val language: String,
    val region: String,
)
