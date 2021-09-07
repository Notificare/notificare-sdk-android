package re.notifica.scannables

import android.content.Context
import androidx.fragment.app.Fragment
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger

public interface NotificareServiceManager {

    public val hasMobileServicesAvailable: Boolean

    public fun getQrCodeScannerFragmentClass(): Class<out Fragment>

    public object Factory {

        private const val FCM_FQN = "re.notifica.scannables.fcm.NotificareServiceManager"
        private const val HMS_FQN = "re.notifica.scannables.hms.NotificareServiceManager"

        internal fun create(context: Context): NotificareServiceManager? {
            val preferredMobileServices = checkNotNull(Notificare.options)
                .preferredMobileServices
                ?.lowercase()

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

            NotificareLogger.warning("No scannables dependencies have been detected. Please include one of the platform-specific scannables packages.")
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
