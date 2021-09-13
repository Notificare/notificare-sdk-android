package re.notifica.push.hms

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareTransport
import re.notifica.push.NotificarePush
import re.notifica.push.hms.internal.NotificareNotificationRemoteMessage
import re.notifica.push.hms.internal.NotificareSystemRemoteMessage
import re.notifica.push.hms.internal.NotificareUnknownRemoteMessage
import re.notifica.push.hms.ktx.isNotificareNotification

public class NotificarePushService : HmsMessageService() {

    override fun onNewToken(token: String) {
        NotificareLogger.info("Received a new HMS token.")

        if (Notificare.deviceManager.currentDevice?.id == token) {
            NotificareLogger.debug("Received token has already been registered. Skipping...")
            return
        }

        if (Notificare.isReady) {
            GlobalScope.launch {
                try {
                    Notificare.deviceManager.registerPushToken(NotificareTransport.HMS, token)
                    NotificareLogger.debug("Registered the device with a HMS token.")
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to register the device with a HMS token.", e)
                }
            }
        } else {
            NotificareLogger.warning("Notificare is not ready. Postponing token registration...")
            NotificarePush.postponedDeviceToken = token
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NotificareLogger.debug("Received a remote notification from HMS.")

        if (NotificarePush.isNotificareNotification(message)) {
            val data = message.dataOfMap
            val isSystemNotification = data["system"] == "1" || data["system"]?.toBoolean() ?: false

            if (isSystemNotification) {
                NotificarePush.handleRemoteMessage(
                    NotificareSystemRemoteMessage(message)
                )
            } else {
                NotificarePush.handleRemoteMessage(
                    NotificareNotificationRemoteMessage(message)
                )
            }
        } else {
            NotificarePush.handleRemoteMessage(
                NotificareUnknownRemoteMessage(message)
            )
        }
    }
}
