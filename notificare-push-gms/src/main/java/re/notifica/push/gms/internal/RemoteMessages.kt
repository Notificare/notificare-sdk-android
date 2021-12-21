package re.notifica.push.gms.internal

import com.google.firebase.messaging.RemoteMessage
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.moshi
import re.notifica.models.NotificareNotification
import re.notifica.push.models.NotificareNotificationRemoteMessage
import re.notifica.push.models.NotificareSystemRemoteMessage
import re.notifica.push.models.NotificareUnknownRemoteMessage

internal fun NotificareUnknownRemoteMessage(message: RemoteMessage): NotificareUnknownRemoteMessage {
    return NotificareUnknownRemoteMessage(
        messageId = message.messageId,
        sentTime = message.sentTime,
        collapseKey = message.collapseKey,
        ttl = message.ttl.toLong(),
        messageType = message.messageType,
        senderId = message.senderId,
        from = message.from,
        to = message.to,
        priority = message.priority,
        originalPriority = message.originalPriority,
        data = message.data,
    )
}

internal fun NotificareSystemRemoteMessage(message: RemoteMessage): NotificareSystemRemoteMessage {
    val ignoreKeys = listOf(
        "id", "notification_id", "notification_type",
        "system", "systemType", "x-sender", "attachment"
    )

    return NotificareSystemRemoteMessage(
        messageId = message.messageId,
        sentTime = message.sentTime,
        collapseKey = message.collapseKey,
        ttl = message.ttl.toLong(),
        id = message.data["id"],
        type = requireNotNull(message.data["systemType"]),
        extra = message.data.filterKeys { !ignoreKeys.contains(it) },
    )
}

internal fun NotificareNotificationRemoteMessage(message: RemoteMessage): NotificareNotificationRemoteMessage {
    val ignoreKeys = listOf(
        "id", "notification_id", "notification_type", "notification_channel", "notification_group", "alert",
        "alert_title", "alert_subtitle", "attachment", "action_category", "inbox_item_id", "inbox_item_visible",
        "inbox_item_expires", "presentation", "notify", "sound", "lights_color", "lights_on", "lights_off", "x-sender"
    )

    return NotificareNotificationRemoteMessage(
        messageId = message.messageId,
        sentTime = message.sentTime,
        collapseKey = message.collapseKey,
        ttl = message.ttl.toLong(),
        // Notification properties
        id = requireNotNull(message.data["id"]),
        notificationId = requireNotNull(message.data["notification_id"]),
        notificationType = message.data["notification_type"] ?: "re.notifica.notification.Alert",
        notificationChannel = message.data["notification_channel"],
        notificationGroup = message.data["notification_group"],
        // Alert properties
        alert = requireNotNull(message.data["alert"]),
        alertTitle = message.data["alert_title"],
        alertSubtitle = message.data["alert_subtitle"],
        attachment = message.data["attachment"]?.let {
            try {
                Notificare.moshi.adapter(NotificareNotification.Attachment::class.java).fromJson(it)
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to parse attachment from remote message.", e)
                null
            }
        },
        //
        actionCategory = message.data["action_category"],
        extra = message.data.filterKeys { !ignoreKeys.contains(it) },
        // Inbox properties
        inboxItemId = message.data["inbox_item_id"],
        inboxItemVisible = message.data["inbox_item_visible"]?.toBoolean() ?: true,
        inboxItemExpires = message.data["inbox_item_expires"]?.toLongOrNull(),
        // Presentation options
        presentation = message.data["presentation"]?.toBoolean() ?: false,
        notify = message.data["notify"] == "1" || message.data["notify"]?.toBoolean() ?: false,
        // Customisation options,
        sound = message.data["sound"],
        lightsColor = message.data["lights_color"],
        lightsOn = message.data["lights_on"]?.toIntOrNull(),
        lightsOff = message.data["lights_off"]?.toIntOrNull(),
    )
}
