package re.notifica.push.internal.network.push

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class DeviceUpdateNotificationSettingsPayload(
    val allowedUI: Boolean,
)

@JsonClass(generateAdapter = true)
internal data class CreateLiveActivityPayload(
    val activity: String,
    val token: String,
    val deviceID: String,
    val topics: List<String>,
)
