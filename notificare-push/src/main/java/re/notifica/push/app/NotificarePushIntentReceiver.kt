package re.notifica.push.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.NotificarePush
import re.notifica.push.models.NotificareSystemNotification
import re.notifica.push.models.NotificareUnknownNotification

open class NotificarePushIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificarePush.INTENT_ACTION_NOTIFICATION_RECEIVED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onNotificationReceived(notification)
            }
            NotificarePush.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED -> {
                val notification: NotificareSystemNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onSystemNotificationReceived(notification)
            }
            NotificarePush.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED -> {
                val notification: NotificareUnknownNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onUnknownNotificationReceived(notification)
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
}
