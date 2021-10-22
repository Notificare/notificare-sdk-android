package re.notifica.push.fcm.internal

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareTransport
import re.notifica.push.fcm.ktx.pushInternal
import re.notifica.push.internal.ServiceManager

@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override val transport: NotificareTransport
        get() = NotificareTransport.GCM

    override fun requestPushToken() {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                NotificareLogger.info("Retrieved FCM token.")

                GlobalScope.launch {
                    try {
                        Notificare.pushInternal().registerPushToken(transport, token = requireNotNull(task.result))
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
}
