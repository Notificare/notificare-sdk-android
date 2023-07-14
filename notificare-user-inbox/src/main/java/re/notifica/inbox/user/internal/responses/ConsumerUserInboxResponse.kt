package re.notifica.inbox.user.internal.responses

import com.squareup.moshi.JsonClass
import re.notifica.inbox.user.models.NotificareUserInboxItem

@JsonClass(generateAdapter = true)
internal data class ConsumerUserInboxResponse(
    val count: Int,
    val unread: Int,
    val items: List<NotificareUserInboxItem>,
)
