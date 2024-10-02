package re.notifica.push.internal.network.push

import com.squareup.moshi.JsonClass
import re.notifica.push.models.NotificareTransport

@JsonClass(generateAdapter = true)
internal data class UpdateDeviceSubscriptionPayload(
    val transport: NotificareTransport,
    val subscriptionId: String?,
    val allowedUI: Boolean,
)

@JsonClass(generateAdapter = true)
internal data class UpdateDeviceNotificationSettingsPayload(
    val allowedUI: Boolean,
)

@JsonClass(generateAdapter = true)
internal data class CreateLiveActivityPayload(
    val activity: String,
    val token: String,
    val deviceID: String,
    val topics: List<String>,
)
