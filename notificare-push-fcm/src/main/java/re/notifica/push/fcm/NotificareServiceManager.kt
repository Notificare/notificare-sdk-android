package re.notifica.push.fcm

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.runBlocking
import re.notifica.NotificareLogger
import re.notifica.models.NotificareTransport
import re.notifica.push.NotificarePush
import re.notifica.push.NotificareServiceManager

@Suppress("unused")
class NotificareServiceManager(
    private val context: Context,
) : NotificareServiceManager {
    override val transport: NotificareTransport
        get() = NotificareTransport.GCM

    override fun registerDeviceToken() {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                NotificareLogger.info("Retrieved FCM token.")

                runBlocking {
                    try {
                        NotificarePush.registerPushToken(transport, token = requireNotNull(task.result))
                        NotificareLogger.debug("Registered the device with a FCM token.")
                    } catch (e: Exception) {
                        NotificareLogger.debug("Failed to register the device with a FCM token.", e)
                    }
                }
            } else if (task.exception != null) {
                NotificareLogger.error("Failed to retrieve FCM token.", task.exception)
            } else {
                NotificareLogger.error("Failed to retrieve FCM token.")
            }
        }
    }

    companion object {
        fun isNotificareNotification(remoteMessage: RemoteMessage): Boolean {
            return remoteMessage.data["x-sender"] == "notificare"
        }
    }
}