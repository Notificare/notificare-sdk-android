package re.notifica.push.models

import android.net.Uri
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareUnknownNotification(
    val messageId: String?,
    val messageType: String?,
    val senderId: String?,
    val collapseKey: String?,
    val from: String?,
    val to: String?,
    val sentTime: Long,
    val ttl: Long,
    val priority: Int,
    val originalPriority: Int,
    val notification: Notification?,
    val data: Map<String, String?>,
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareUnknownNotification::class.java)

        public fun fromJson(json: JSONObject): NotificareUnknownNotification {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Notification(
        val title: String?,
        val titleLocalizationKey: String?,
        val titleLocalizationArgs: List<String>?,
        val body: String?,
        val bodyLocalizationKey: String?,
        val bodyLocalizationArgs: List<String>?,
        val icon: String?,
        val imageUrl: Uri?,
        val sound: String?,
        val tag: String?,
        val color: String?,
        val clickAction: String?,
        val channelId: String?,
        val link: Uri?,
        val ticker: String?,
        val sticky: Boolean,
        val localOnly: Boolean,
        val defaultSound: Boolean,
        val defaultVibrateSettings: Boolean,
        val defaultLightSettings: Boolean,
        val notificationPriority: Int?,
        val visibility: Int?,
        val notificationCount: Int?,
        val eventTime: Long?,
        val lightSettings: List<Int>?,
        val vibrateSettings: List<Long>?,
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(Notification::class.java)

            public fun fromJson(json: JSONObject): Notification {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }
}
