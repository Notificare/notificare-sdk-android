package re.notifica.push.hms

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import kotlinx.coroutines.runBlocking
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareTransport

class NotificarePushService : HmsMessageService() {

    override fun onNewToken(token: String) {
        NotificareLogger.info("Received a new HMS token.")

        if (Notificare.deviceManager.currentDevice?.id == token) {
            NotificareLogger.debug("Received token has already been registered. Skipping...")
            return
        }

        if (Notificare.isReady) {
            runBlocking {
                try {
                    Notificare.deviceManager.registerToken(NotificareTransport.HMS, token)
                    NotificareLogger.debug("Registered the device with a HMS token.")
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to register the device with a HMS token.", e)
                }
            }
        } else {
            NotificareLogger.warning("Notificare is not ready. Skipping token registration.")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        NotificareLogger.debug("Received a remote notification.")

        if (NotificareServiceManager.isNotificareNotification(remoteMessage)) {
            NotificareLogger.debug("IS NOTIFICARE.")
        } else {
            NotificareLogger.debug("IS NOT NOTIFICARE.")
        }
    }
}
