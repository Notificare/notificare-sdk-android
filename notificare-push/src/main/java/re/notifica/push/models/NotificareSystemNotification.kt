package re.notifica.push.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareSystemNotification(
    val id: String,
    val type: String,
    val extra: Map<String, String?>,
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareSystemNotification::class.java)

        public fun fromJson(json: JSONObject): NotificareSystemNotification {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
