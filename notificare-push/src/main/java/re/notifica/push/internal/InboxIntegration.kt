package re.notifica.push.internal

import android.content.Intent
import androidx.core.os.bundleOf
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.models.NotificareNotificationRemoteMessage

internal object InboxIntegration {

    private const val INBOX_RECEIVER_FQN = "re.notifica.inbox.NotificareInboxSystemReceiver"

    private const val INTENT_ACTION_INBOX_RELOAD = "re.notifica.inbox.intent.action.Reload"
    private const val INTENT_ACTION_INBOX_NOTIFICATION_RECEIVED = "re.notifica.inbox.intent.action.NotificationReceived"
    private const val INTENT_ACTION_INBOX_MARK_ITEM_AS_READ = "re.notifica.inbox.intent.action.MarkItemAsRead"

    private const val INTENT_EXTRA_INBOX_NOTIFICATION_RECEIVED_BUNDLE = "re.notifica.inbox.intent.extra.InboxBundle"
    private const val INTENT_EXTRA_INBOX_ITEM_ID = "re.notifica.inbox.intent.extra.InboxItemId"

    fun reloadInbox() {
        try {
            val klass = Class.forName(INBOX_RECEIVER_FQN)
            val intent = Intent(Notificare.requireContext(), klass).apply {
                action = INTENT_ACTION_INBOX_RELOAD
            }

            Notificare.requireContext().sendBroadcast(intent)
        } catch (e: Exception) {
            if (e is ClassNotFoundException) {
                NotificareLogger.debug(
                    "The inbox module is not available. Please include it if you want to leverage the inbox capabilities."
                )
                return
            }

            NotificareLogger.debug("Failed to send an inbox broadcast.", e)
        }
    }

    fun addItemToInbox(message: NotificareNotificationRemoteMessage, notification: NotificareNotification) {
        if (message.inboxItemId == null) {
            NotificareLogger.debug(
                "Received a remote message without an inbox item id. Inbox functionality is disabled."
            )
            return
        }

        try {
            val klass = Class.forName(INBOX_RECEIVER_FQN)
            val intent = Intent(Notificare.requireContext(), klass).apply {
                action = INTENT_ACTION_INBOX_NOTIFICATION_RECEIVED

                putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                putExtra(
                    INTENT_EXTRA_INBOX_NOTIFICATION_RECEIVED_BUNDLE,
                    bundleOf(
                        "inboxItemId" to message.inboxItemId,
                        "inboxItemTime" to message.sentTime,
                        "inboxItemVisible" to message.inboxItemVisible,
                        "inboxItemExpires" to message.inboxItemExpires
                    )
                )
            }

            Notificare.requireContext().sendBroadcast(intent)
        } catch (e: Exception) {
            if (e is ClassNotFoundException) {
                NotificareLogger.debug(
                    "The inbox module is not available. Please include it if you want to leverage the inbox capabilities."
                )
                return
            }

            NotificareLogger.debug("Failed to send an inbox broadcast.", e)
        }
    }

    fun markItemAsRead(message: NotificareNotificationRemoteMessage) {
        if (message.inboxItemId == null) {
            NotificareLogger.debug(
                "Received a remote message without an inbox item id. Inbox functionality is disabled."
            )
            return
        }

        try {
            val klass = Class.forName(INBOX_RECEIVER_FQN)
            val intent = Intent(Notificare.requireContext(), klass).apply {
                action = INTENT_ACTION_INBOX_MARK_ITEM_AS_READ

                putExtra(INTENT_EXTRA_INBOX_ITEM_ID, message.inboxItemId)
            }

            Notificare.requireContext().sendBroadcast(intent)
        } catch (e: Exception) {
            if (e is ClassNotFoundException) {
                NotificareLogger.debug(
                    "The inbox module is not available. Please include it if you want to leverage the inbox capabilities."
                )
                return
            }

            NotificareLogger.debug("Failed to send an inbox broadcast.", e)
        }
    }
}
