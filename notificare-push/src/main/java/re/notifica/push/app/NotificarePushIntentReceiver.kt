package re.notifica.push.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.NotificareLogger
import re.notifica.push.models.NotificareSystemNotification
import re.notifica.push.models.NotificareUnknownNotification

open class NotificarePushIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Actions.NOTIFICATION_RECEIVED -> {
                // TODO: handle notification parameter
                onNotificationReceived()
            }
            Actions.SYSTEM_NOTIFICATION_RECEIVED -> {
                val notification: NotificareSystemNotification = requireNotNull(
                    intent.getParcelableExtra(Extras.NOTIFICATION)
                )

                onSystemNotificationReceived(notification)
            }
            Actions.UNKNOWN_NOTIFICATION_RECEIVED -> {
                val notification: NotificareUnknownNotification = requireNotNull(
                    intent.getParcelableExtra(Extras.NOTIFICATION)
                )

                onUnknownNotificationReceived(notification)
            }
        }
    }

    protected open fun onNotificationReceived() {
        NotificareLogger.info("Received a notification, please override onNotificationReceived if you want to receive these intents.")
    }

    protected open fun onSystemNotificationReceived(notification: NotificareSystemNotification) {
        NotificareLogger.info("Received a system notification, please override onSystemNotificationReceived if you want to receive these intents.")
    }

    protected open fun onUnknownNotificationReceived(notification: NotificareUnknownNotification) {
        NotificareLogger.info("Received an unknown notification, please override onUnknownNotificationReceived if you want to receive these intents.")
    }

    object Actions {
        const val NOTIFICATION_RECEIVED = "re.notifica.intent.action.NotificationReceived"
        const val SYSTEM_NOTIFICATION_RECEIVED = "re.notifica.intent.action.SystemNotificationReceived"
        const val UNKNOWN_NOTIFICATION_RECEIVED = "re.notifica.intent.action.UnknownNotificationReceived"
    }

    object Extras {
        const val NOTIFICATION = "re.notifica.intent.extra.Notification"
    }
}
