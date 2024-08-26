package re.notifica.inbox.user.internal.responses

import com.squareup.moshi.JsonClass
import java.util.Date
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.utilities.moshi.UseDefaultsWhenNull
import re.notifica.models.NotificareNotification

@JsonClass(generateAdapter = true)
internal data class RawUserInboxResponse(
    val count: Int,
    val unread: Int,
    val inboxItems: List<RawUserInboxItem>,
) {

    @UseDefaultsWhenNull
    @JsonClass(generateAdapter = true)
    internal data class RawUserInboxItem(
        val _id: String,
        val notification: String,
        val type: String,
        val time: Date,
        val title: String?,
        val subtitle: String?,
        val message: String,
        val attachment: NotificareNotification.Attachment?,
        val extra: Map<String, Any> = mapOf(),
        val opened: Boolean = false,
        val expires: Date? = null,
    ) {

        internal fun toModel(): NotificareUserInboxItem {
            return NotificareUserInboxItem(
                id = _id,
                notification = NotificareNotification(
                    partial = true,
                    id = notification,
                    type = type,
                    time = time,
                    title = title,
                    subtitle = subtitle,
                    message = message,
                    attachments = attachment?.let { listOf(it) } ?: listOf(),
                    extra = extra,
                ),
                time = time,
                opened = opened,
                expires = expires,
            )
        }
    }
}
