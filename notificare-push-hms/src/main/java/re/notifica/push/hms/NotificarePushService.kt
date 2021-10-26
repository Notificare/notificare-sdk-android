package re.notifica.push.hms

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.ktx.device
import re.notifica.models.NotificareTransport
import re.notifica.push.hms.internal.NotificareNotificationRemoteMessage
import re.notifica.push.hms.internal.NotificareSystemRemoteMessage
import re.notifica.push.hms.internal.NotificareUnknownRemoteMessage
import re.notifica.push.hms.ktx.deviceInternal
import re.notifica.push.hms.ktx.isNotificareNotification
import re.notifica.push.hms.ktx.pushInternal
import re.notifica.push.ktx.push

public class NotificarePushService : HmsMessageService() {

    override fun onNewToken(token: String) {
        NotificareLogger.info("Received a new HMS token.")

        if (Notificare.device().currentDevice?.id == token) {
            NotificareLogger.debug("Received token has already been registered. Skipping...")
            return
        }

        if (Notificare.isReady) {
            GlobalScope.launch {
                try {
                    Notificare.deviceInternal().registerPushToken(NotificareTransport.HMS, token)
                    NotificareLogger.debug("Registered the device with a HMS token.")
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to register the device with a HMS token.", e)
                }
            }
        } else {
            NotificareLogger.warning("Notificare is not ready. Postponing token registration...")
            Notificare.pushInternal().postponedDeviceToken = token
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NotificareLogger.debug("Received a remote notification from HMS.")

        if (Notificare.push().isNotificareNotification(message)) {
            val data = message.dataOfMap
            val isSystemNotification = data["system"] == "1" || data["system"]?.toBoolean() ?: false

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
