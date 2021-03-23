package re.notifica.push.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
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
        // Close the notification drawer
        Notificare.requireContext().sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        GlobalScope.launch {
            try {
                // Log the notification open event.
                Notificare.eventsManager.logNotificationOpened(notification.id)

                @Suppress("NAME_SHADOWING") val notification = if (notification.partial) {
                    Notificare.fetchNotification(message.id)
                } else {
                    notification
                }

                if (action != null && action.type == "callback" && !action.camera && (!action.keyboard || responseText != null)) {
                    // TODO handle the action
                    return@launch
                }

                // TODO notify listeners

                // Notify the consumer's intent receiver.
                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), NotificarePush.intentReceiver)
                        .setAction(NotificarePush.INTENT_ACTION_NOTIFICATION_OPENED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                )

                // Notify the consumer's custom activity about the notification open event.
                val notificationIntent = Intent()
                    .setAction(NotificarePush.INTENT_ACTION_NOTIFICATION_OPENED)
                    .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                    .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .setPackage(Notificare.requireContext().packageName)

                val canHandleNotificationOpenIntent =
                    notificationIntent.resolveActivity(Notificare.requireContext().packageManager) != null

                if (canHandleNotificationOpenIntent) {
                    // Notification handled by custom activity in package
                    Notificare.requireContext().startActivity(notificationIntent)
                } else {
                    NotificareLogger.warning("Could not find an activity with the '${NotificarePush.INTENT_ACTION_NOTIFICATION_OPENED}' action.")
                }
            } catch (e: Exception) {
                NotificareLogger.error("Failed to fetch notification.", e)
            }
        }
    }
}
