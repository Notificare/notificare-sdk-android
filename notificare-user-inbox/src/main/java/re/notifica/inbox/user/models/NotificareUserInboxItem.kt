package re.notifica.inbox.user.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.models.NotificareNotification
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareUserInboxItem(
    val id: String,
    val notification: NotificareNotification,
    val time: Date,
    val opened: Boolean,
    val expires: Date?,
) : Parcelable {
    public companion object {
        private val adapter by lazy {
            Notificare.moshi.adapter(NotificareUserInboxItem::class.java)
        }

        public fun fromJson(json: JSONObject): NotificareUserInboxItem {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }
}
