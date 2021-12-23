package re.notifica.inbox.internal.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import re.notifica.inbox.internal.network.push.InboxResponse
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.models.NotificareNotification
import java.util.*

@Entity(
    tableName = "inbox"
)
internal data class InboxItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "notification_id") val notificationId: String,
    @ColumnInfo(name = "notification") var notification: NotificareNotification,
    @ColumnInfo(name = "time") val time: Date,
    @ColumnInfo(name = "opened") var opened: Boolean,
    @ColumnInfo(name = "expires") val expires: Date?,
    @ColumnInfo(name = "visible") val visible: Boolean,
) {

    fun toInboxItem(): NotificareInboxItem {
        return NotificareInboxItem(
            id = id,
            notification = notification,
            time = time,
            opened = opened,
            expires = expires,
        )
    }

    companion object Factory {
        fun from(item: NotificareInboxItem, visible: Boolean): InboxItemEntity {
            return InboxItemEntity(
                id = item.id,
                notificationId = item.notification.id,
                notification = item.notification,
                time = item.time,
                opened = item.opened,
                expires = item.expires,
                visible = visible,
            )
        }

        fun from(item: InboxResponse.InboxItem): InboxItemEntity {
            return InboxItemEntity(
                id = item.id,
                notificationId = item.notificationId,
                notification = NotificareNotification(
                    partial = true,
                    id = item.notificationId,
                    type = item.type,
                    time = item.time,
                    title = item.title,
                    subtitle = item.subtitle,
                    message = item.message,
                    attachments = item.attachment?.let { listOf(it) } ?: listOf(),
                    extra = item.extra,
                ),
                time = item.time,
                opened = item.opened,
                visible = item.visible,
                expires = item.expires,
            )
        }
    }
}
