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
    @ColumnInfo(name = "notification") val notification: NotificareNotification,
    @ColumnInfo(name = "time") val time: Date,
    @ColumnInfo(name = "opened") val opened: Boolean,
    @ColumnInfo(name = "visible") val visible: Boolean,
    @ColumnInfo(name = "expires") val expires: Date?,
) {

    fun toInboxItem(): NotificareInboxItem {
        return NotificareInboxItem(
            id = id,
            _notification = notification,
            time = time,
            _opened = opened,
            visible = visible,
            expires = expires,
        )
    }

    companion object Factory {
        fun from(item: NotificareInboxItem): InboxItemEntity {
            return InboxItemEntity(
                id = item.id,
                notificationId = item.notification.id,
                notification = item.notification,
                time = item.time,
                opened = item.opened,
                visible = item.visible,
                expires = item.expires,
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
