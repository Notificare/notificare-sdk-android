package re.notifica.push.internal

import com.google.firebase.messaging.RemoteMessage
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.models.NotificareNotification
import re.notifica.push.models.NotificareNotificationRemoteMessage
import re.notifica.push.models.NotificareSystemRemoteMessage
import re.notifica.push.models.NotificareUnknownNotification
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
        notification = message.notification?.let {
            NotificareUnknownNotification.Notification(
                title = it.title,
                titleLocalizationKey = it.titleLocalizationKey,
                titleLocalizationArgs = it.titleLocalizationArgs?.toList(),
                body = it.body,
                bodyLocalizationKey = it.bodyLocalizationKey,
                bodyLocalizationArgs = it.bodyLocalizationArgs?.toList(),
                icon = it.icon,
                imageUrl = it.imageUrl,
                sound = it.sound,
                tag = it.tag,
                color = it.color,
                clickAction = it.clickAction,
                channelId = it.channelId,
                link = it.link,
                ticker = it.ticker,
                sticky = it.sticky,
                localOnly = it.localOnly,
                defaultSound = it.defaultSound,
                defaultVibrateSettings = it.defaultVibrateSettings,
                defaultLightSettings = it.defaultLightSettings,
                notificationPriority = it.notificationPriority,
                visibility = it.visibility,
                notificationCount = it.notificationCount,
                eventTime = it.eventTime,
                lightSettings = it.lightSettings?.toList(),
                vibrateSettings = it.vibrateTimings?.toList(),
            )
        },
        data = message.data,
    )
}

internal fun NotificareSystemRemoteMessage(message: RemoteMessage): NotificareSystemRemoteMessage {
    val ignoreKeys = listOf(
        "id",
        "notification_id",
        "notification_type",
        "system",
        "systemType",
        "attachment",
    )

    val extras = message.data.filterKeys { key ->
        !ignoreKeys.contains(key) && !key.startsWith("x-")
    }

    return NotificareSystemRemoteMessage(
        messageId = message.messageId,
        sentTime = message.sentTime,
        collapseKey = message.collapseKey,
        ttl = message.ttl.toLong(),
        id = message.data["id"],
        type = requireNotNull(message.data["systemType"]),
        extra = extras,
    )
}

internal fun NotificareNotificationRemoteMessage(message: RemoteMessage): NotificareNotificationRemoteMessage {
    val ignoreKeys = listOf(
        "id", "notification_id", "notification_type", "notification_channel", "notification_group", "alert",
        "alert_title", "alert_subtitle", "attachment", "action_category", "inbox_item_id", "inbox_item_visible",
        "inbox_item_expires", "presentation", "notify", "sound", "lights_color", "lights_on", "lights_off",
    )

    val extras = message.data.filterKeys { key ->
        !ignoreKeys.contains(key) && !key.startsWith("x-")
    }

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
                logger.warning("Failed to parse attachment from remote message.", e)
                null
            }
        },
        //
        actionCategory = message.data["action_category"],
        extra = extras,
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
