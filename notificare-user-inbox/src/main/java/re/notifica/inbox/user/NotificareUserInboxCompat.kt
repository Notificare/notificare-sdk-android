package re.notifica.inbox.user

import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.inbox.user.ktx.userInbox
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.inbox.user.models.NotificareUserInboxResponse
import re.notifica.models.NotificareNotification

public object NotificareUserInboxCompat {

    @JvmStatic
    public fun parseResponse(json: String): NotificareUserInboxResponse {
        return Notificare.userInbox().parseResponse(json)
    }

    @JvmStatic
    public fun parseResponse(json: JSONObject): NotificareUserInboxResponse {
        return Notificare.userInbox().parseResponse(json)
    }

    @JvmStatic
    public fun open(item: NotificareUserInboxItem, callback: NotificareCallback<NotificareNotification>) {
        Notificare.userInbox().open(item, callback)
    }

    @JvmStatic
    public fun markAsRead(item: NotificareUserInboxItem, callback: NotificareCallback<Unit>) {
        Notificare.userInbox().markAsRead(item, callback)
    }

    @JvmStatic
    public fun remove(item: NotificareUserInboxItem, callback: NotificareCallback<Unit>) {
        Notificare.userInbox().remove(item, callback)
    }
}
