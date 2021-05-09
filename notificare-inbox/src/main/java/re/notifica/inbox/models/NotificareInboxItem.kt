package re.notifica.inbox.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import java.util.*

@JsonClass(generateAdapter = true)
data class NotificareInboxItem internal constructor(
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

    fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    companion object {
        private val adapter = Notificare.moshi.adapter(NotificareInboxItem::class.java)

        fun fromJson(json: JSONObject): NotificareInboxItem {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
