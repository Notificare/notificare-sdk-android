package re.notifica.push.internal.network.push

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class DeviceUpdateNotificationSettingsPayload(
    val allowedUI: Boolean,
)
