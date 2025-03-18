package re.notifica.inbox.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import java.util.Date
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.models.NotificareNotification

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareInboxItem(
    val id: String,
    val notification: NotificareNotification,
    val time: Date,
    val opened: Boolean,
    val expires: Date?,
) : Parcelable {

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
