package re.notifica.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import re.notifica.Notificare

public typealias NotificareEventData = Map<String, Any?>

@JsonClass(generateAdapter = true)
public data class NotificareEvent(
    val type: String,
    val timestamp: Long,
    @Json(name = "deviceID") val deviceId: String?,
    @Json(name = "sessionID") val sessionId: String?,
    @Json(name = "notification") val notificationId: String?,
    @Json(name = "userID") val userId: String?,
    val data: NotificareEventData?
) {

    internal companion object {
        internal val dataAdapter: JsonAdapter<NotificareEventData> by lazy {
            Notificare.moshi.adapter(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            )
        }
    }
}
