package re.notifica.push

import android.content.Context
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareTransport
import java.util.*

interface NotificareServiceManager {

    val transport: NotificareTransport

    val hasMobileServicesAvailable: Boolean

    fun registerDeviceToken()

    object Factory {

        private const val FCM_FQN = "re.notifica.push.fcm.NotificareServiceManager"
        private const val HMS_FQN = "re.notifica.push.hms.NotificareServiceManager"

        fun create(context: Context): NotificareServiceManager? {
            val preferredMobileServices = checkNotNull(Notificare.options)
                .preferredMobileServices
                ?.toLowerCase(Locale.ROOT)

            when (preferredMobileServices) {
                "google" -> {
                    val fcm: NotificareServiceManager? = create(context, FCM_FQN)
                    if (fcm != null) {
                        NotificareLogger.info("Detected preferred FCM peer dependency. Setting it as the target platform.")
                        return fcm
                    } else {
                        NotificareLogger.warning("Preferred Google Play Services not available.")
                    }
                }
                "huawei" -> {
                    val hms: NotificareServiceManager? = create(context, HMS_FQN)
                    if (hms != null) {
                        NotificareLogger.info("Detected preferred HMS peer dependency. Setting it as the target platform.")
                        return hms
                    } else {
                        NotificareLogger.warning("Preferred Huawei Mobile Services not available.")
                    }
                }
            }

            val fcm: NotificareServiceManager? = create(context, FCM_FQN)
            if (fcm != null) {
                NotificareLogger.info("Detected FCM peer dependency. Setting it as the target platform.")
                return fcm
            }

            val hms: NotificareServiceManager? = create(context, HMS_FQN)
            if (hms != null) {
                NotificareLogger.info("Detected HMS peer dependency. Setting it as the target platform.")
                return hms
            }

            NotificareLogger.warning("No push dependencies have been detected. Please include one of the platform-specific push packages.")
            return null
        }

        private fun create(context: Context, fqn: String): NotificareServiceManager? {
            return try {
                val klass = Class.forName(fqn)
                klass.getConstructor(Context::class.java).newInstance(context) as? NotificareServiceManager
            } catch (e: Exception) {
                null
            }
        }
    }
}
