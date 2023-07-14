package re.notifica.inbox.user

import org.json.JSONObject
import re.notifica.NotificareCallback
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.inbox.user.models.NotificareUserInboxResponse
import re.notifica.models.NotificareNotification

public interface NotificareUserInbox {

    public fun parseResponse(json: String): NotificareUserInboxResponse

    public fun parseResponse(json: JSONObject): NotificareUserInboxResponse

    public suspend fun open(item: NotificareUserInboxItem): NotificareNotification

    public fun open(item: NotificareUserInboxItem, callback: NotificareCallback<NotificareNotification>)

    public suspend fun markAsRead(item: NotificareUserInboxItem)

    public fun markAsRead(item: NotificareUserInboxItem, callback: NotificareCallback<Unit>)

    public suspend fun remove(item: NotificareUserInboxItem)

    public fun remove(item: NotificareUserInboxItem, callback: NotificareCallback<Unit>)
}
