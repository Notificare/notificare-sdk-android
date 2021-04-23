package re.notifica.push.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import re.notifica.models.NotificareNotification
import java.util.*

interface NotificareRemoteMessage {
    val messageId: String?
    val sentTime: Long
    val collapseKey: String?
    val ttl: Long
}

data class NotificareUnknownRemoteMessage(
    override val messageId: String?,
    override val sentTime: Long,
    override val collapseKey: String?,
    override val ttl: Long,

    val messageType: String?,
    val senderId: String?,
    val from: String?,
    val to: String?,
    val priority: Int,
    val originalPriority: Int,
    val data: Map<String, String?>,
) : NotificareRemoteMessage {

    fun toNotification(): NotificareUnknownNotification {
        return NotificareUnknownNotification(
            messageId = messageId,
            messageType = messageType,
            senderId = senderId,
            collapseKey = collapseKey,
            from = from,
            to = to,
            sentTime = sentTime,
            ttl = ttl,
            priority = priority,
            originalPriority = originalPriority,
            data = data,
        )
    }
}

data class NotificareSystemRemoteMessage(
    override val messageId: String?,
    override val sentTime: Long,
    override val collapseKey: String?,
    override val ttl: Long,

    // Specific properties
    val id: String,
    val type: String,
    val extra: Map<String, String>,
) : NotificareRemoteMessage

@Parcelize
data class NotificareNotificationRemoteMessage(
    override val messageId: String?,
    override val sentTime: Long,
    override val collapseKey: String?,
    override val ttl: Long,

    // Notification properties
    val id: String,
    val notificationId: String,
    val notificationType: String,
    val notificationChannel: String?,
    val notificationGroup: String?,

    // Alert properties
    val alert: String,
    val alertTitle: String?,
    val alertSubtitle: String?,
    val attachment: NotificareNotification.Attachment?,

    val actionCategory: String?,
    val extra: Map<String, String>,

    // Inbox properties
    val inboxItemId: String?,
    val inboxItemVisible: Boolean,
    val inboxItemExpires: Long?,

    // Presentation options
    val presentation: Boolean,
    val notify: Boolean,

    // Customisation options
    val sound: String?,
    val lightsColor: String?,
    val lightsOn: Int?,
    val lightsOff: Int?,
) : NotificareRemoteMessage, Parcelable {

    fun toNotification(): NotificareNotification {
        return NotificareNotification(
            id = notificationId,
            partial = true,
            type = notificationType,
            time = Date(sentTime),
            title = alertTitle,
            subtitle = alertSubtitle,
            message = alert,
            content = emptyList(),
            actions = emptyList(),
            attachments = attachment?.let { listOf(it) } ?: emptyList(),
            extra = extra,
        )
    }
}
