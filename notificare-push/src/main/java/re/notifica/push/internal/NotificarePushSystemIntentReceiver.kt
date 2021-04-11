package re.notifica.push.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.RemoteInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.NotificarePush
import re.notifica.push.models.NotificareNotificationRemoteMessage

internal class NotificarePushSystemIntentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificarePush.INTENT_ACTION_REMOTE_MESSAGE_OPENED -> {
                val message: NotificareNotificationRemoteMessage = requireNotNull(
                    intent.getParcelableExtra(NotificarePush.INTENT_EXTRA_REMOTE_MESSAGE)
                )

                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action? = intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
                val responseText = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(NotificarePush.INTENT_EXTRA_TEXT_RESPONSE)
                    ?.toString()

                onRemoteMessageOpened(message, notification, action, responseText)
            }
        }
    }

    private fun onRemoteMessageOpened(
        message: NotificareNotificationRemoteMessage,
        notification: NotificareNotification,
        action: NotificareNotification.Action?,
        responseText: String?
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Log the notification open event.
                Notificare.eventsManager.logNotificationOpened(notification.id)

                @Suppress("NAME_SHADOWING") val notification = if (notification.partial) {
                    Notificare.fetchNotification(message.id)
                } else {
                    notification
                }

                if (action != null && action.type == NotificareNotification.Action.TYPE_CALLBACK && !action.camera && (!action.keyboard || responseText != null)) {
                    NotificareLogger.debug("Handling a notification action without UI.")
                    sendQuickResponse(notification, action, responseText)

                    // Remove the notification from the notification center.
                    Notificare.removeNotificationFromNotificationCenter(notification)

                    // Notify the inbox to mark the item as read.
                    markItemAsRead(message)

                    return@launch
                }

                // Close the notification drawer
                Notificare.requireContext().sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

                // TODO notify listeners

//                // Notify the consumer's intent receiver.
//                Notificare.requireContext().sendBroadcast(
//                    Intent(Notificare.requireContext(), NotificarePush.intentReceiver)
//                        .setAction(NotificarePush.INTENT_ACTION_NOTIFICATION_OPENED)
//                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
//                )

                // Notify the consumer's custom activity about the notification open event.
                val notificationIntent = Intent()
                    .setAction(
                        if (action == null) NotificarePush.INTENT_ACTION_NOTIFICATION_OPENED
                        else NotificarePush.INTENT_ACTION_ACTION_OPENED
                    )
                    .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                    .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .setPackage(Notificare.requireContext().packageName)

                if (notificationIntent.resolveActivity(Notificare.requireContext().packageManager) != null) {
                    // Notification handled by custom activity in package
                    Notificare.requireContext().startActivity(notificationIntent)
                } else {
                    NotificareLogger.warning("Could not find an activity with the '${notificationIntent.action}' action.")
                }
            } catch (e: Exception) {
                NotificareLogger.error("Failed to fetch notification.", e)
            }
        }
    }

    private fun sendQuickResponse(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        responseText: String?
    ) {
        val targetUri = action.target?.let { Uri.parse(it) }
        if (targetUri == null || targetUri.scheme == null || targetUri.host == null) {
            // NotificarePushUI.shared.delegate?.notificare(NotificarePushUI.shared, didExecuteAction: action, for: notification)
            sendQuickResponseAction(notification, action, responseText)

            return
        }

        val params = mutableMapOf<String, String>()
        params["notificationID"] = notification.id
        params["label"] = action.label
        responseText?.let { params["message"] = it }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                Notificare.callNotificationReplyWebhook(targetUri, params)

                // NotificarePushUI.shared.delegate?.notificare(NotificarePushUI.shared, didExecuteAction: self.action, for: self.notification)
            } catch (e: Exception) {
                // NotificarePushUI.shared.delegate?.notificare(NotificarePushUI.shared, didFailToExecuteAction: self.action, for: self.notification, error: error)
                NotificareLogger.debug("Failed to call the notification reply webhook.", e)
            }

            sendQuickResponseAction(notification, action, responseText)
        }
    }

    private fun sendQuickResponseAction(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        responseText: String?
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Notificare.createNotificationReply(
                    notification = notification,
                    action = action,
                    message = responseText,
                    media = null,
                    mimeType = null,
                )
            } catch (e: Exception) {
                NotificareLogger.debug("Failed to create a notification reply.", e)
            }
        }
    }

    private fun markItemAsRead(message: NotificareNotificationRemoteMessage) {
        if (message.inboxItemId == null) {
            return
        }

        try {
            val klass = Class.forName(NotificarePush.INBOX_RECEIVER_CLASS_NAME)
            val intent = Intent(Notificare.requireContext(), klass).apply {
                action = NotificarePush.INTENT_ACTION_INBOX_MARK_ITEM_AS_READ
                putExtra(NotificarePush.INTENT_EXTRA_INBOX_ITEM_ID, message.inboxItemId)
            }

            Notificare.requireContext().sendBroadcast(intent)
        } catch (e: Exception) {
            NotificareLogger.debug("Failed to send an inbox broadcast.", e)
        }
    }
}
