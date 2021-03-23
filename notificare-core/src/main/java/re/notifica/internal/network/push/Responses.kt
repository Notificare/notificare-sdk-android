package re.notifica.internal.network.push

import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareUserData

@JsonClass(generateAdapter = true)
internal data class ApplicationResponse(
    val application: NotificareApplication
)

@JsonClass(generateAdapter = true)
internal data class DeviceDoNotDisturbResponse(
    val dnd: NotificareDoNotDisturb?,
)

@JsonClass(generateAdapter = true)
internal data class DeviceTagsResponse(
    val tags: List<String>
)

@JsonClass(generateAdapter = true)
internal data class DeviceUserDataResponse(
    val userData: NotificareUserData?,
)

@JsonClass(generateAdapter = true)
data class NotificationResponse(
    val notification: NotificareNotification,
)
