package re.notifica.inbox.user.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi

@Parcelize
@JsonClass(generateAdapter = false)
public data class NotificareUserInboxResponse(
    val count: Int,
    val unread: Int,
    val items: List<NotificareUserInboxItem>,
) : Parcelable {
    public companion object {
        private val adapter by lazy {
            Notificare.moshi.adapter(NotificareUserInboxResponse::class.java)
        }

        public fun fromJson(json: JSONObject): NotificareUserInboxResponse {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }
}
