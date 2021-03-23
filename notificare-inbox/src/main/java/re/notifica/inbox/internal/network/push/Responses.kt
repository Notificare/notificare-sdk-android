package re.notifica.inbox.internal.network.push

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import re.notifica.internal.moshi.UseDefaultsWhenNull
import re.notifica.models.NotificareNotification
import java.util.*

@JsonClass(generateAdapter = true)
data class InboxResponse(
    val inboxItems: List<InboxItem>,
    val count: Int,
    val unread: Int,
) {

    @UseDefaultsWhenNull
    @JsonClass(generateAdapter = true)
    data class InboxItem(
        @Json(name = "_id") val id: String,
        @Json(name = "notification") val notificationId: String,
        val type: String,
        val time: Date,
        val title: String?,
        val subtitle: String?,
        val message: String,
        val attachment: NotificareNotification.Attachment?,
        val extra: Map<String, Any> = mapOf(),
        val opened: Boolean = false,
        val visible: Boolean = true,
        val expires: Date? = null,
    )
}
