package re.notifica.push.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.push.NotificarePush
import re.notifica.push.app.NotificarePushIntentReceiver
import re.notifica.push.logNotificationOpened
import re.notifica.push.models.NotificareNotificationRemoteMessage

internal class NotificarePushSystemIntentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Actions.REMOTE_MESSAGE_OPENED -> {
                val message: NotificareNotificationRemoteMessage = requireNotNull(
                    intent.getParcelableExtra(Extras.REMOTE_MESSAGE)
                )

                onRemoteMessageOpened(message)
            }
        }
    }

    private fun onRemoteMessageOpened(message: NotificareNotificationRemoteMessage) {
        GlobalScope.launch {
            try {
                val notification = Notificare.fetchNotification(message.id)

                // Log the notification open event.
                Notificare.eventsManager.logNotificationOpened(notification.id)

                // TODO notify listeners

                // Notify the consumer's intent receiver.
                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), NotificarePush.intentReceiver)
                        .setAction(NotificarePushIntentReceiver.Actions.NOTIFICATION_OPENED)
                        .putExtra(NotificarePushIntentReceiver.Extras.NOTIFICATION, notification)
                )

                // Notify the consumer's custom activity about the notification open event.
                val notificationIntent = Intent()
                    .setAction(NotificarePush.INTENT_ACTION_NOTIFICATION_OPENED)
                    .putExtra(NotificarePush.INTENT_EXTRA_NOTIFICATION, notification)
                    // .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
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

    object Actions {
        const val REMOTE_MESSAGE_OPENED = "re.notifica.intent.action.RemoteMessageOpened"
    }

    object Extras {
        const val REMOTE_MESSAGE = "re.notifica.intent.extra.RemoteMessage"
    }
}
