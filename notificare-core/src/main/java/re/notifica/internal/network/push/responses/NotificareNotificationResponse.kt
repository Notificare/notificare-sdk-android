package re.notifica.internal.network.push.responses

import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareNotification

@JsonClass(generateAdapter = true)
data class NotificareNotificationResponse(
    val notification: NotificareNotification,
)
