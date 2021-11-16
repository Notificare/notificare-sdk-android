package re.notifica.push.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.ktx.device
import re.notifica.models.NotificareTransport
import re.notifica.push.fcm.internal.NotificareNotificationRemoteMessage
import re.notifica.push.fcm.internal.NotificareSystemRemoteMessage
import re.notifica.push.fcm.internal.NotificareUnknownRemoteMessage
import re.notifica.push.fcm.ktx.deviceInternal
import re.notifica.push.fcm.ktx.isNotificareNotification
import re.notifica.push.fcm.ktx.pushInternal
import re.notifica.push.ktx.push

public class NotificarePushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        NotificareLogger.info("Received a new FCM token.")

        if (Notificare.device().currentDevice?.id == token) {
            NotificareLogger.debug("Received token has already been registered. Skipping...")
            return
        }

        if (Notificare.isReady) {
            GlobalScope.launch {
                try {
                    Notificare.deviceInternal().registerPushToken(NotificareTransport.GCM, token)
                    NotificareLogger.debug("Registered the device with a FCM token.")
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to register the device with a FCM token.", e)
                }
            }
        } else {
            NotificareLogger.warning("Notificare is not ready. Postponing token registration...")
            Notificare.pushInternal().postponedDeviceToken = token
        }
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
