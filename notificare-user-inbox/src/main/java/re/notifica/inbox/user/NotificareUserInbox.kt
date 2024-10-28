package re.notifica.inbox.user

import org.json.JSONObject
import re.notifica.NotificareCallback
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.inbox.user.models.NotificareUserInboxResponse
import re.notifica.models.NotificareNotification

public interface NotificareUserInbox {

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
    public fun parseResponse(json: String): NotificareUserInboxResponse

    /**
     * Parses a [JSONObject] to produce a [NotificareUserInboxResponse].
     *
     * This method takes a [JSONObject] and converts it into a structured [NotificareUserInboxResponse]
     *
     * @param json The [JSONObject] representing the user inbox response.
     * @return A [NotificareUserInboxResponse] object parsed from the provided JSON object.
     *
     * @see [NotificareUserInboxResponse]
     */
    public fun parseResponse(json: JSONObject): NotificareUserInboxResponse

    /**
     * Opens an inbox item and retrieves its associated notification.
     *
     * This is a suspending function that opens the provided [NotificareUserInboxItem] and returns the
     * associated [NotificareNotification]. This operation marks the item as read by sending a notification
     * open event.
     *
     * @param item The [NotificareUserInboxItem] to be opened.
     * @return The [NotificareNotification] associated with the opened inbox item.
     *
     * @see [NotificareUserInboxItem]
     * @see [NotificareNotification]
     */
    public suspend fun open(item: NotificareUserInboxItem): NotificareNotification

    /**
     * Opens an inbox item and retrieves its associated notification with a callback.
     *
     * This method opens the provided [NotificareUserInboxItem] and invokes the provided [NotificareCallback] with the
     * associated [NotificareNotification]. This operation marks the item as read by sending a notification open event.
     *
     * @param item The [NotificareUserInboxItem] to be opened.
     * @param callback The [NotificareCallback] to be invoked with the [NotificareNotification] or an error.
     *
     * @see [NotificareUserInboxItem]
     * @see [NotificareNotification]
     */
    public fun open(item: NotificareUserInboxItem, callback: NotificareCallback<NotificareNotification>)

    /**
     * Marks an inbox item as read.
     *
     * This is a suspending function that updates the status of the provided [NotificareUserInboxItem] to read.
     *
     * @param item The [NotificareUserInboxItem] to mark as read.
     *
     * @see [NotificareUserInboxItem]
     */
    public suspend fun markAsRead(item: NotificareUserInboxItem)

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
    public fun markAsRead(item: NotificareUserInboxItem, callback: NotificareCallback<Unit>)

    /**
     * Removes an inbox item from the user's inbox.
     *
     * This is a suspending function that deletes the provided [NotificareUserInboxItem] from the user's inbox.
     *
     * @param item The [NotificareUserInboxItem] to be removed.
     */
    public suspend fun remove(item: NotificareUserInboxItem)

    /**
     * Removes an inbox item from the user's inbox with a callback.
     *
     * This method deletes the provided [NotificareUserInboxItem] and invokes the provided [NotificareCallback]
     * upon success or failure.
     *
     * @param item The [NotificareUserInboxItem] to be removed.
     * @param callback The [NotificareCallback] to be invoked with success or an error.
     */
    public fun remove(item: NotificareUserInboxItem, callback: NotificareCallback<Unit>)
}
