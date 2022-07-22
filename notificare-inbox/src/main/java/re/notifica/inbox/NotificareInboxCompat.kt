package re.notifica.inbox

import androidx.lifecycle.LiveData
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.inbox.ktx.inbox
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.models.NotificareNotification
import java.util.*

public object NotificareInboxCompat {

    @JvmStatic
    public val items: SortedSet<NotificareInboxItem>
        get() = Notificare.inbox().items

    @JvmStatic
    public val badge: Int
        get() = Notificare.inbox().badge

    @JvmStatic
    public val observableItems: LiveData<SortedSet<NotificareInboxItem>> =
        Notificare.inbox().observableItems

    @JvmStatic
    public val observableBadge: LiveData<Int> = Notificare.inbox().observableBadge

    @JvmStatic
    public fun refresh() {
        Notificare.inbox().refresh()
    }

    @JvmStatic
    public fun open(
        item: NotificareInboxItem,
        callback: NotificareCallback<NotificareNotification>
    ) {
        Notificare.inbox().open(item, callback)
    }

    @JvmStatic
    public fun markAsRead(item: NotificareInboxItem, callback: NotificareCallback<Unit>) {
        Notificare.inbox().markAsRead(item, callback)
    }

    @JvmStatic
    public fun markAllAsRead(callback: NotificareCallback<Unit>) {
        Notificare.inbox().markAllAsRead(callback)
    }

    @JvmStatic
    public fun remove(item: NotificareInboxItem, callback: NotificareCallback<Unit>) {
        Notificare.inbox().remove(item, callback)
    }

    @JvmStatic
    public fun clear(callback: NotificareCallback<Unit>) {
        Notificare.inbox().clear(callback)
    }
}
