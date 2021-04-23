package re.notifica.push.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareTransport
import re.notifica.push.NotificarePush
import re.notifica.push.models.NotificareNotificationRemoteMessage
import re.notifica.push.models.NotificareSystemRemoteMessage
import re.notifica.push.models.NotificareUnknownRemoteMessage

class NotificarePushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        NotificareLogger.info("Received a new FCM token.")

        if (Notificare.deviceManager.currentDevice?.id == token) {
            NotificareLogger.debug("Received token has already been registered. Skipping...")
            return
        }

        if (Notificare.isReady) {
            runBlocking {
                try {
                    Notificare.deviceManager.registerPushToken(NotificareTransport.GCM, token)
                    NotificareLogger.debug("Registered the device with a FCM token.")
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to register the device with a FCM token.", e)
                }
            }
        } else {
            NotificareLogger.warning("Notificare is not ready. Skipping token registration.")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NotificareLogger.debug("Received a remote notification from FCM.")

        if (NotificareServiceManager.isNotificareNotification(message)) {
            val isSystemNotification = message.data["system"] == "1" ||
                message.data["system"]?.toBoolean() ?: false

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
        senderId = message.senderId,
        from = message.from,
        to = message.to,
        priority = message.priority,
        originalPriority = message.originalPriority,
        data = message.data,
    )
}

private fun NotificareSystemRemoteMessage(message: RemoteMessage): NotificareSystemRemoteMessage {
    val ignoreKeys = listOf(
        "id", "notification_id", "notification_type",
        "system", "systemType", "x-sender", "attachment"
    )

    return NotificareSystemRemoteMessage(
        messageId = message.messageId,
        sentTime = message.sentTime,
        collapseKey = message.collapseKey,
        ttl = message.ttl.toLong(),
        id = requireNotNull(message.data["id"]),
        type = requireNotNull(message.data["systemType"]),
        extra = message.data.filterKeys { !ignoreKeys.contains(it) },
    )
}

private fun NotificareNotificationRemoteMessage(message: RemoteMessage): NotificareNotificationRemoteMessage {
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
