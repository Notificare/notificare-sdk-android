package re.notifica.inbox

import androidx.lifecycle.LiveData
import java.util.SortedSet
import re.notifica.NotificareCallback
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.models.NotificareNotification

public interface NotificareInbox {

    /**
     * A sorted set of all [NotificareInboxItem], sorted by the timestamp.
     */
    public val items: SortedSet<NotificareInboxItem>

    /**
     * The current badge count, representing the number of unread inbox items.
     */
    public val badge: Int

    /**
     * A [LiveData] object observing changes to inbox items, suitable for real-time UI updates to reflect inbox state
     * changes.
     */
    public val observableItems: LiveData<SortedSet<NotificareInboxItem>>

    /**
     * A [LiveData] object observing changes to the badge count, providing real-time updates when the unread count
     * changes.
     */
    public val observableBadge: LiveData<Int>

    /**
     * Refreshes the inbox data, ensuring the items and badge count reflect the latest server state.
     */
    public fun refresh()

    /**
     * Opens a specified inbox item, marking it as read and returning the associated notification.
     *
     * @param item The [NotificareInboxItem] to open.
     * @return The [NotificareNotification] associated with the inbox item.
     */
    public suspend fun open(item: NotificareInboxItem): NotificareNotification

    /**
     * Opens a specified inbox item, marking it as read and returning the associated notification, with a callback.
     *
     * @param item The [NotificareInboxItem] to open.
     * @param callback A callback to handle the resulting [NotificareNotification] or any errors encountered.
     */
    public fun open(item: NotificareInboxItem, callback: NotificareCallback<NotificareNotification>)

    /**
     * Marks the specified inbox item as read.
     *
     * @param item The [NotificareInboxItem] to mark as read.
     */
    public suspend fun markAsRead(item: NotificareInboxItem)

    /**
     * Marks the specified inbox item as read, with a callback.
     *
     * @param item The [NotificareInboxItem] to mark as read.
     */
    public fun markAsRead(item: NotificareInboxItem, callback: NotificareCallback<Unit>)

    /**
     * Marks all inbox items as read.
     */
    public suspend fun markAllAsRead()

    /**
     * Marks all inbox items as read, with a callback.
     */
    public fun markAllAsRead(callback: NotificareCallback<Unit>)

    /**
     * Permanently removes the specified inbox item from the inbox.
     *
     * @param item The [NotificareInboxItem] to remove.
     */
    public suspend fun remove(item: NotificareInboxItem)

    /**
     * Permanently removes the specified inbox item from the inbox, with a callback.
     *
     * @param item The [NotificareInboxItem] to remove.
     */
    public fun remove(item: NotificareInboxItem, callback: NotificareCallback<Unit>)

    /**
     * Clears all inbox items, permanently deleting them from the inbox.
     */
    public suspend fun clear()

    /**
     * Clears all inbox items, permanently deleting them from the inbox, with a callback.
     */
    public fun clear(callback: NotificareCallback<Unit>)
}
