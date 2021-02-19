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

                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), NotificarePush.intentReceiver)
                        .setAction(NotificarePushIntentReceiver.Actions.NOTIFICATION_OPENED)
                        .putExtra(NotificarePushIntentReceiver.Extras.NOTIFICATION, notification)
                )
            } catch (e: Exception) {
                NotificareLogger.error("Failed to fetch notification.", e)
            }
        }
    }

    object Actions {
        const val REMOTE_MESSAGE_OPENED = "re.notifica.intent.action.RemoteMessageOpened"

        // const val REMOTE_MESSAGE_DELETED = "re.notifica.intent.action.RemoteMessageDeleted"
        // const val RELEVANCE_REMOTE_MESSAGE_OPENED = "re.notifica.intent.action.RelevanceRemoteMessageOpened"
        // const val RELEVANCE_REMOTE_MESSAGE_DELETED = "re.notifica.intent.action.RelevanceRemoteMessageDeleted"
    }

    object Extras {
        const val REMOTE_MESSAGE = "re.notifica.intent.extra.RemoteMessage"
    }
}
