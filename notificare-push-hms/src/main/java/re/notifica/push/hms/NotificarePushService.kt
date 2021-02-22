package re.notifica.push.hms

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import kotlinx.coroutines.runBlocking
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareTransport
import re.notifica.push.NotificarePush
import re.notifica.push.models.NotificareNotificationRemoteMessage
import re.notifica.push.models.NotificareSystemRemoteMessage
import re.notifica.push.models.NotificareUnknownRemoteMessage

class NotificarePushService : HmsMessageService() {

    override fun onNewToken(token: String) {
        NotificareLogger.info("Received a new HMS token.")

        if (Notificare.deviceManager.currentDevice?.id == token) {
            NotificareLogger.debug("Received token has already been registered. Skipping...")
            return
        }

        if (Notificare.isReady) {
            runBlocking {
                try {
                    Notificare.deviceManager.registerPushToken(NotificareTransport.HMS, token)
                    NotificareLogger.debug("Registered the device with a HMS token.")
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to register the device with a HMS token.", e)
                }
            }
        } else {
            NotificareLogger.warning("Notificare is not ready. Skipping token registration.")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NotificareLogger.debug("Received a remote notification from HMS.")

        if (NotificareServiceManager.isNotificareNotification(message)) {
            val data = message.dataOfMap
            val isSystemNotification = data["system"] == "1" || data["system"]?.toBoolean() ?: false

            if (isSystemNotification) {
                NotificarePush.handleRemoteMessage(
                    NotificareSystemRemoteMessage(message)
                )
            } else {
                NotificarePush.handleRemoteMessage(
                    NotificareNotificationRemoteMessage(message)
                )
            }
        } else {
            NotificarePush.handleRemoteMessage(
                NotificareUnknownRemoteMessage(message)
            )
        }
    }
}


private fun NotificareUnknownRemoteMessage(message: RemoteMessage): NotificareUnknownRemoteMessage {
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
        data = message.dataOfMap,
    )
}

private fun NotificareSystemRemoteMessage(message: RemoteMessage): NotificareSystemRemoteMessage {
    return NotificareSystemRemoteMessage(
        messageId = message.messageId,
        sentTime = message.sentTime,
        collapseKey = message.collapseKey,
        ttl = message.ttl.toLong(),
        systemType = requireNotNull(message.dataOfMap["systemType"])
    )
}

private fun NotificareNotificationRemoteMessage(message: RemoteMessage): NotificareNotificationRemoteMessage {
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
