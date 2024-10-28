package re.notifica.inbox

import androidx.lifecycle.LiveData
import java.util.SortedSet
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.inbox.ktx.inbox
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.models.NotificareNotification

public object NotificareInboxCompat {

    /**
     * A sorted set of all inbox items, sorted by the timestamp.
     */
    @JvmStatic
    public val items: SortedSet<NotificareInboxItem>
        get() = Notificare.inbox().items

    /**
     * The current badge count, representing the number of unread inbox items.
     */
    @JvmStatic
    public val badge: Int
        get() = Notificare.inbox().badge

    /**
     * A [LiveData] object observing changes to inbox items, suitable for real-time UI updates to reflect inbox state
     * changes.
     */
    @JvmStatic
    public val observableItems: LiveData<SortedSet<NotificareInboxItem>> =
        Notificare.inbox().observableItems

    /**
     * A [LiveData] object observing changes to the badge count, providing real-time updates when the unread count
     * changes.
     */
    @JvmStatic
    public val observableBadge: LiveData<Int> = Notificare.inbox().observableBadge

    /**
     * Refreshes the inbox data, ensuring the items and badge count reflect the latest server state.
     */
    @JvmStatic
    public fun refresh() {
        Notificare.inbox().refresh()
    }

    /**
     * Opens a specified inbox item, marking it as read and returning the associated notification, with a callback.
     *
     * @param item The inbox item to open.
     * @return The [NotificareNotification] associated with the inbox item.
     */
    @JvmStatic
    public fun open(item: NotificareInboxItem, callback: NotificareCallback<NotificareNotification>) {
        Notificare.inbox().open(item, callback)
    }

    /**
     * Marks the specified inbox item as read, with a callback.
     *
     * @param item The inbox item to mark as read.
     */
    @JvmStatic
    public fun markAsRead(item: NotificareInboxItem, callback: NotificareCallback<Unit>) {
        Notificare.inbox().markAsRead(item, callback)
    }

    /**
     * Marks all inbox items as read, with a callback.
     */
    @JvmStatic
    public fun markAllAsRead(callback: NotificareCallback<Unit>) {
        Notificare.inbox().markAllAsRead(callback)
    }

    /**
     * Permanently removes the specified inbox item from the inbox, with a callback.
     *
     * @param item The inbox item to remove.
     */
    @JvmStatic
    public fun remove(item: NotificareInboxItem, callback: NotificareCallback<Unit>) {
        Notificare.inbox().remove(item, callback)
    }

    /**
     * Clears all inbox items, permanently deleting them from the inbox, with a callback.
     */
    @JvmStatic
    public fun clear(callback: NotificareCallback<Unit>) {
        Notificare.inbox().clear(callback)
    }
}
