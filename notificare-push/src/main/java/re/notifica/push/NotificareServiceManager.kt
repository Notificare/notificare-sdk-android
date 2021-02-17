package re.notifica.push

import android.content.Context
import re.notifica.NotificareLogger
import re.notifica.models.NotificareTransport

interface NotificareServiceManager {

    val transport: NotificareTransport

    fun registerDeviceToken()

    object Factory {

        private const val FCM_FQN = "re.notifica.push.fcm.NotificareServiceManager"
        private const val HMS_FQN = "re.notifica.push.hms.NotificareServiceManager"

        fun create(context: Context): NotificareServiceManager? {
            val fcm: NotificareServiceManager? = create(context, FCM_FQN)
            val hms: NotificareServiceManager? = create(context, HMS_FQN)

            if (fcm != null && hms != null) {
                NotificareLogger.warning("We've detected multiple push platforms implemented. Please consider only including the most appropriate for your target platform.")
            }

            if (fcm != null) {
                NotificareLogger.info("Detected FCM peer dependency. Setting it as the target platform.")
                return fcm
            }

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
