package re.notifica.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.ktx.*
import re.notifica.push.models.NotificareSystemNotification
import re.notifica.push.models.NotificareUnknownNotification

public open class NotificarePushIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Notificare.INTENT_ACTION_NOTIFICATION_RECEIVED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onNotificationReceived(context, notification)
            }
            Notificare.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED -> {
                val notification: NotificareSystemNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onSystemNotificationReceived(context, notification)
            }
            Notificare.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED -> {
                val notification: NotificareUnknownNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onUnknownNotificationReceived(context, notification)
            }
            Notificare.INTENT_ACTION_NOTIFICATION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onNotificationOpened(context, notification)
            }
            Notificare.INTENT_ACTION_ACTION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
                )

                onActionOpened(context, notification, action)
            }
        }
    }

    protected open fun onNotificationReceived(context: Context, notification: NotificareNotification) {
        NotificareLogger.info("Received a notification, please override onNotificationReceived if you want to receive these intents.")
    }

    protected open fun onSystemNotificationReceived(context: Context, notification: NotificareSystemNotification) {
        NotificareLogger.info("Received a system notification, please override onSystemNotificationReceived if you want to receive these intents.")
    }

    protected open fun onUnknownNotificationReceived(context: Context, notification: NotificareUnknownNotification) {
        NotificareLogger.info("Received an unknown notification, please override onUnknownNotificationReceived if you want to receive these intents.")
    }

    protected open fun onNotificationOpened(context: Context, notification: NotificareNotification) {
        NotificareLogger.debug("Opened a notification, please override onNotificationOpened if you want to receive these intents.")
    }

    protected open fun onActionOpened(
        context: Context,
        notification: NotificareNotification,
        action: NotificareNotification.Action
    ) {
        NotificareLogger.debug("Opened a notification action, please override onActionOpened if you want to receive these intents.")
    }
}
