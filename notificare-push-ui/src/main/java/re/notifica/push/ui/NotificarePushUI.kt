package re.notifica.push.ui

import android.app.Activity
import android.content.Intent
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareTransport
import re.notifica.push.NotificarePush
import re.notifica.push.ui.fragments.NotificareAlertFragment
import re.notifica.push.ui.fragments.NotificareImageFragment
import re.notifica.push.ui.fragments.NotificareUrlFragment
import re.notifica.push.ui.fragments.NotificareVideoFragment

object NotificarePushUI {

    fun presentNotification(activity: Activity, notification: NotificareNotification) {
        val intent = Intent(Notificare.requireContext(), NotificationActivity::class.java)
            .putExtra(NotificarePush.INTENT_EXTRA_NOTIFICATION, notification)
//            .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
//            .setPackage(Notificare.requireContext().packageName)

        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }

    internal fun getFragmentCanonicalClassName(notification: NotificareNotification): String? {
        val type = NotificareNotification.NotificationType.from(notification.type) ?: run {
            NotificareLogger.warning("Unhandled notification type '${notification.type}'.")
            return null
        }

        return when (type) {
            NotificareNotification.NotificationType.NONE -> {
                NotificareLogger.debug("Attempting to create a fragment for a notification of type 'none'. This type contains to visual interface.")
                return null
            }
            NotificareNotification.NotificationType.ALERT -> NotificareAlertFragment::class.java.canonicalName
            NotificareNotification.NotificationType.WEB_VIEW -> null
            NotificareNotification.NotificationType.URL -> NotificareUrlFragment::class.java.canonicalName
            NotificareNotification.NotificationType.URL_SCHEME -> {
                NotificareLogger.debug("Attempting to create a fragment for a notification of type 'urlScheme'. This type contains to visual interface.")
                return null
            }
            NotificareNotification.NotificationType.RATE -> {
                val serviceManager = NotificarePush.serviceManager ?: run {
                    NotificareLogger.warning("No push dependencies have been detected. Please include one of the platform-specific push packages.")
                    return null
                }

                when (serviceManager.transport) {
                    NotificareTransport.NOTIFICARE -> null
                    NotificareTransport.GCM -> "re.notifica.push.ui.fcm.NotificareRateFragment"
                    NotificareTransport.HMS -> "re.notifica.push.ui.hms.NotificareRateFragment"
                }
            }
            NotificareNotification.NotificationType.IMAGE -> NotificareImageFragment::class.java.canonicalName
            NotificareNotification.NotificationType.MAP -> {
                val serviceManager = NotificarePush.serviceManager ?: run {
                    NotificareLogger.warning("No push dependencies have been detected. Please include one of the platform-specific push packages.")
                    return null
                }

                when (serviceManager.transport) {
                    NotificareTransport.NOTIFICARE -> null
                    NotificareTransport.GCM -> "re.notifica.push.ui.fcm.NotificareMapFragment"
                    NotificareTransport.HMS -> "re.notifica.push.ui.hms.NotificareMapFragment"
                }
            }
            NotificareNotification.NotificationType.PASSBOOK -> {
                // TODO: handle passbook notification
                return null
            }
            NotificareNotification.NotificationType.STORE -> {
                val serviceManager = NotificarePush.serviceManager ?: run {
                    NotificareLogger.warning("No push dependencies have been detected. Please include one of the platform-specific push packages.")
                    return null
                }

                when (serviceManager.transport) {
                    NotificareTransport.NOTIFICARE -> null
                    NotificareTransport.GCM -> "re.notifica.push.ui.fcm.NotificareStoreFragment"
                    NotificareTransport.HMS -> "re.notifica.push.ui.hms.NotificareStoreFragment"
                }
            }
            NotificareNotification.NotificationType.VIDEO -> NotificareVideoFragment::class.java.canonicalName
        }
    }
}
