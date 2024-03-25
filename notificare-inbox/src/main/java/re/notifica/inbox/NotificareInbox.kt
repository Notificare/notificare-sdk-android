package re.notifica.inbox

import androidx.lifecycle.LiveData
import java.util.SortedSet
import re.notifica.NotificareCallback
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.models.NotificareNotification

public interface NotificareInbox {

    public val items: SortedSet<NotificareInboxItem>
    public val badge: Int

    public val observableItems: LiveData<SortedSet<NotificareInboxItem>>
    public val observableBadge: LiveData<Int>

    public fun refresh()

    public suspend fun open(item: NotificareInboxItem): NotificareNotification

    public fun open(item: NotificareInboxItem, callback: NotificareCallback<NotificareNotification>)

    public suspend fun markAsRead(item: NotificareInboxItem)

    public fun markAsRead(item: NotificareInboxItem, callback: NotificareCallback<Unit>)

    public suspend fun markAllAsRead()

    public fun markAllAsRead(callback: NotificareCallback<Unit>)

    public suspend fun remove(item: NotificareInboxItem)

    public fun remove(item: NotificareInboxItem, callback: NotificareCallback<Unit>)

    public suspend fun clear()

    public fun clear(callback: NotificareCallback<Unit>)
}
