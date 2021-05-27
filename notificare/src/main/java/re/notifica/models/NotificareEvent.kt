package re.notifica.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// todo consider additional types
typealias NotificareEventData = Map<String, String?>

@JsonClass(generateAdapter = true)
data class NotificareEvent(
    val type: String,
    val timestamp: Long,
    @Json(name = "deviceID") val deviceId: String?,
    @Json(name = "sessionID") val sessionId: String?,
    @Json(name = "notification") val notificationId: String?,
    @Json(name = "userID") val userId: String?,
    val data: NotificareEventData?
)
