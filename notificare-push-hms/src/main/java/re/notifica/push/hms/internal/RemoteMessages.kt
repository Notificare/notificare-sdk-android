package re.notifica.push.hms.internal

import com.huawei.hms.push.RemoteMessage
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
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
        senderId = null,
        from = message.from,
        to = message.to,
        priority = message.urgency,
        originalPriority = message.originalUrgency,
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
                sticky = it.isAutoCancel,
                localOnly = it.isLocalOnly,
                defaultSound = it.isDefaultSound,
                defaultVibrateSettings = it.isDefaultVibrate,
                defaultLightSettings = it.isDefaultLight,
                notificationPriority = it.importance,
                visibility = it.visibility,
                notificationCount = it.badgeNumber,
                eventTime = it.`when`,
                lightSettings = it.lightSettings?.toList(),
                vibrateSettings = it.vibrateConfig?.toList(),
            )
        },
        data = message.dataOfMap,
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
        id = message.dataOfMap["id"],
        type = requireNotNull(message.dataOfMap["systemType"]),
        extra = message.dataOfMap.filterKeys { !ignoreKeys.contains(it) },
    )
}

internal fun NotificareNotificationRemoteMessage(message: RemoteMessage): NotificareNotificationRemoteMessage {
    val data = message.dataOfMap

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
        id = requireNotNull(data["id"]),
        notificationId = requireNotNull(data["notification_id"]),
        notificationType = data["notification_type"] ?: "re.notifica.notification.Alert",
        notificationChannel = data["notification_channel"],
        notificationGroup = data["notification_group"],
        // Alert properties
        alert = requireNotNull(data["alert"]),
        alertTitle = data["alert_title"],
        alertSubtitle = data["alert_subtitle"],
        attachment = data["attachment"]?.let {
            try {
                Notificare.moshi.adapter(NotificareNotification.Attachment::class.java).fromJson(it)
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to parse attachment from remote message.", e)
                null
            }
        },
        //
        actionCategory = data["action_category"],
        extra = data.filterKeys { !ignoreKeys.contains(it) },
        // Inbox properties
        inboxItemId = data["inbox_item_id"],
        inboxItemVisible = data["inbox_item_visible"]?.toBoolean() ?: true,
        inboxItemExpires = data["inbox_item_expires"]?.toLongOrNull(),
        // Presentation options
        presentation = data["presentation"]?.toBoolean() ?: false,
        notify = data["notify"] == "1" || data["notify"]?.toBoolean() ?: false,
        // Customisation options,
        sound = data["sound"],
        lightsColor = data["lights_color"],
        lightsOn = data["lights_on"]?.toIntOrNull(),
        lightsOff = data["lights_off"]?.toIntOrNull(),
    )
}
