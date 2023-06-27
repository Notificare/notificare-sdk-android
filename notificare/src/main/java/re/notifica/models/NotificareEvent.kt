package re.notifica.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi

public typealias NotificareEventData = Map<String, Any?>

@JsonClass(generateAdapter = true)
public data class NotificareEvent(
    val type: String,
    val timestamp: Long,
    @Json(name = "deviceID") val deviceId: String,
    @Json(name = "sessionID") val sessionId: String?,
    @Json(name = "notification") val notificationId: String?,
    @Json(name = "userID") val userId: String?,
    val data: NotificareEventData?
) {

    public companion object {
        internal val dataAdapter: JsonAdapter<NotificareEventData> by lazy {
            Notificare.moshi.adapter(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            )
        }

        public fun createData(json: JSONObject): NotificareEventData {
            return requireNotNull(dataAdapter.fromJson(json.toString()))
        }
    }
}
