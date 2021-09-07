package re.notifica.inbox.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.models.NotificareNotification
import java.util.*

@JsonClass(generateAdapter = true)
public data class NotificareInboxItem internal constructor(
    val id: String,
    @Json(name = "notification") internal var _notification: NotificareNotification,
    val time: Date,
    @Json(name = "opened") internal var _opened: Boolean,
    internal val visible: Boolean,
    val expires: Date?,
) {

    val notification: NotificareNotification
        get() = _notification

    val opened: Boolean
        get() = _opened

    internal val expired: Boolean
        get() {
            val expiresAt = expires ?: return false
            return expiresAt.before(Date())
        }

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareInboxItem::class.java)

        public fun fromJson(json: JSONObject): NotificareInboxItem {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
