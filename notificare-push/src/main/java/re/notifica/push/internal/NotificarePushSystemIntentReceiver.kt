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

    companion object {
        const val INTENT_ACTION_QUICK_RESPONSE = "re.notifica.intent.action.NotificationQuickResponse"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            INTENT_ACTION_QUICK_RESPONSE -> {
                val message: NotificareNotificationRemoteMessage = requireNotNull(
                    intent.getParcelableExtra(NotificarePush.INTENT_EXTRA_REMOTE_MESSAGE)
                )

                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
                )

                val responseText = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(NotificarePush.INTENT_EXTRA_TEXT_RESPONSE)
                    ?.toString()

                onQuickResponse(message, notification, action, responseText)
            }
        }
    }

    private fun onQuickResponse(
        message: NotificareNotificationRemoteMessage,
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        responseText: String?
    ) {
        // Log the notification open event.
        Notificare.eventsManager.logNotificationOpened(notification.id)

        GlobalScope.launch(Dispatchers.IO) {
            @Suppress("NAME_SHADOWING")
            val notification = try {
                if (notification.partial) {
                    Notificare.fetchNotification(message.id)
                } else {
                    notification
                }
            } catch (e: Exception) {
                NotificareLogger.error("Failed to fetch notification.", e)
                return@launch
            }

            NotificareLogger.debug("Handling a notification action without UI.")
            sendQuickResponse(notification, action, responseText)

            // Remove the notification from the notification center.
            Notificare.removeNotificationFromNotificationCenter(notification)

            // Notify the inbox to mark the item as read.
            InboxIntegration.markItemAsRead(message)
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
}
