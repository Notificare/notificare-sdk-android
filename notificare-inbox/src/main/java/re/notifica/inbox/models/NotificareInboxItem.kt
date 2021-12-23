package re.notifica.inbox.models

import android.os.Parcelable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.models.NotificareNotification
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareInboxItem(
    val id: String,
    val notification: NotificareNotification,
    val time: Date,
    val opened: Boolean,
    val expires: Date?,
) : Parcelable {
    public companion object
}

// region JSON: NotificareInboxItem

public val NotificareInboxItem.Companion.adapter: JsonAdapter<NotificareInboxItem>
    get() = Notificare.moshi.adapter(NotificareInboxItem::class.java)

public fun NotificareInboxItem.Companion.fromJson(json: JSONObject): NotificareInboxItem {
    val jsonStr = json.toString()
    return requireNotNull(adapter.fromJson(jsonStr))
}

public fun NotificareInboxItem.toJson(): JSONObject {
    val jsonStr = NotificareInboxItem.adapter.toJson(this)
    return JSONObject(jsonStr)
}

// endregion
