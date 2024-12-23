package re.notifica.inbox.user

import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.inbox.user.ktx.userInbox
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.inbox.user.models.NotificareUserInboxResponse
import re.notifica.models.NotificareNotification

public object NotificareUserInboxCompat {

    /**
     * Parses a JSON string to produce a [NotificareUserInboxResponse].
     *
     * This method takes a raw JSON string and converts it into a structured [NotificareUserInboxResponse].
     *
     * @param json The JSON string representing the user inbox response.
     * @return A [NotificareUserInboxResponse] object parsed from the provided JSON string.
     *
     * @see [NotificareUserInboxResponse]
     */
    @JvmStatic
    public fun parseResponse(json: String): NotificareUserInboxResponse {
        return Notificare.userInbox().parseResponse(json)
    }

    /**
     * Parses a [JSONObject] to produce a [NotificareUserInboxResponse].
     *
     * This method takes a [JSONObject] and converts it into a structured [NotificareUserInboxResponse].
     *
     * @param json The [JSONObject] representing the user inbox response.
     * @return A [NotificareUserInboxResponse] object parsed from the provided JSON object.
     *
     * @see [NotificareUserInboxResponse]
     */
    @JvmStatic
    public fun parseResponse(json: JSONObject): NotificareUserInboxResponse {
        return Notificare.userInbox().parseResponse(json)
    }

    /**
     * Opens an inbox item and retrieves its associated notification with a callback.
     *
     * This method opens the provided [NotificareUserInboxItem] and invokes the provided [NotificareCallback] with the
     * associated [NotificareNotification]. This operation marks the item as read.
     *
     * @param item The [NotificareUserInboxItem] to be opened.
     * @param callback The [NotificareCallback] to be invoked with the [NotificareNotification] or an error.
     *
     * @see [NotificareUserInboxItem]
     * @see [NotificareNotification]
     */
    @JvmStatic
    public fun open(item: NotificareUserInboxItem, callback: NotificareCallback<NotificareNotification>) {
        Notificare.userInbox().open(item, callback)
    }

    /**
     * Marks an inbox item as read with a callback.
     *
     * This method marks the provided [NotificareUserInboxItem] as read and invokes the provided [NotificareCallback]
     * upon success or failure.
     *
     * @param item The [NotificareUserInboxItem] to mark as read.
     *
     * @see [NotificareUserInboxItem]
     */
    @JvmStatic
    public fun markAsRead(item: NotificareUserInboxItem, callback: NotificareCallback<Unit>) {
        Notificare.userInbox().markAsRead(item, callback)
    }

    /**
     * Removes an inbox item from the user's inbox with a callback.
     *
     * This method deletes the provided [NotificareUserInboxItem] and invokes the provided [NotificareCallback]
     * upon success or failure.
     *
     * @param item The [NotificareUserInboxItem] to be removed.
     * @param callback The [NotificareCallback] to be invoked with success or an error.
     */
    @JvmStatic
    public fun remove(item: NotificareUserInboxItem, callback: NotificareCallback<Unit>) {
        Notificare.userInbox().remove(item, callback)
    }
}
