package re.notifica.push.gms

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.push.gms.internal.NotificareNotificationRemoteMessage
import re.notifica.push.gms.internal.NotificareSystemRemoteMessage
import re.notifica.push.gms.internal.NotificareUnknownRemoteMessage
import re.notifica.push.gms.ktx.isNotificareNotification
import re.notifica.push.gms.ktx.pushInternal
import re.notifica.push.ktx.push
import re.notifica.push.models.NotificareTransport

public open class NotificarePushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Notificare.pushInternal().handleNewToken(NotificareTransport.GCM, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NotificareLogger.debug("Received a remote notification from FCM.")

        if (Notificare.push().isNotificareNotification(message)) {
            val isSystemNotification = message.data["system"] == "1" ||
                message.data["system"]?.toBoolean() ?: false

            if (isSystemNotification) {
                Notificare.pushInternal().handleRemoteMessage(
                    NotificareSystemRemoteMessage(message)
                )
            } else {
                Notificare.pushInternal().handleRemoteMessage(
                    NotificareNotificationRemoteMessage(message)
                )
            }
        } else {
            Notificare.pushInternal().handleRemoteMessage(
                NotificareUnknownRemoteMessage(message)
            )
        }
    }
}
