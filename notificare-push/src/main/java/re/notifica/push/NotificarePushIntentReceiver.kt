package re.notifica.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.ktx.push
import re.notifica.push.models.NotificareSystemNotification
import re.notifica.push.models.NotificareUnknownNotification

public open class NotificarePushIntentReceiver : BroadcastReceiver() {

    public companion object {
        // Intent actions
        internal const val INTENT_ACTION_REMOTE_MESSAGE_OPENED: String =
            "re.notifica.intent.action.RemoteMessageOpened"
        internal const val INTENT_ACTION_NOTIFICATION_RECEIVED: String =
            "re.notifica.intent.action.NotificationReceived"
        internal const val INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED: String =
            "re.notifica.intent.action.SystemNotificationReceived"
        internal const val INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED: String =
            "re.notifica.intent.action.UnknownNotificationReceived"

        // Intent extras
        internal const val INTENT_EXTRA_REMOTE_MESSAGE: String = "re.notifica.intent.extra.RemoteMessage"
        internal const val INTENT_EXTRA_TEXT_RESPONSE: String = "re.notifica.intent.extra.TextResponse"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            INTENT_ACTION_NOTIFICATION_RECEIVED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onNotificationReceived(notification)
            }
            INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED -> {
                val notification: NotificareSystemNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onSystemNotificationReceived(notification)
            }
            INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED -> {
                val notification: NotificareUnknownNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onUnknownNotificationReceived(notification)
            }
            Notificare.push().INTENT_ACTION_NOTIFICATION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onNotificationOpened(notification)
            }
            Notificare.push().INTENT_ACTION_ACTION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
                )

                onActionOpened(notification, action)
            }
        }
    }

    protected open fun onNotificationReceived(notification: NotificareNotification) {
        NotificareLogger.info("Received a notification, please override onNotificationReceived if you want to receive these intents.")
    }

    protected open fun onSystemNotificationReceived(notification: NotificareSystemNotification) {
        NotificareLogger.info("Received a system notification, please override onSystemNotificationReceived if you want to receive these intents.")
    }

    protected open fun onUnknownNotificationReceived(notification: NotificareUnknownNotification) {
        NotificareLogger.info("Received an unknown notification, please override onUnknownNotificationReceived if you want to receive these intents.")
    }

    protected open fun onNotificationOpened(notification: NotificareNotification) {
        NotificareLogger.debug("Opened a notification, please override onNotificationOpened if you want to receive these intents.")
    }

    protected open fun onActionOpened(notification: NotificareNotification, action: NotificareNotification.Action) {
        NotificareLogger.debug("Opened a notification action, please override onActionOpened if you want to receive these intents.")
    }
}
