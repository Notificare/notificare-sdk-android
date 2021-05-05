package re.notifica.push.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareTransport
import re.notifica.push.NotificarePush
import re.notifica.push.ui.actions.*
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.app.NotificarePushUIIntentReceiver
import re.notifica.push.ui.notifications.fragments.*

object NotificarePushUI {

    const val SDK_VERSION = BuildConfig.SDK_VERSION

    private const val CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX = ".notificare.fileprovider"

    internal val contentFileProviderAuthority: String
        get() = "${Notificare.requireContext().packageName}$CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX"

    var notificationActivity: Class<out NotificationActivity> = NotificationActivity::class.java
    var intentReceiver: Class<out NotificarePushUIIntentReceiver> = NotificarePushUIIntentReceiver::class.java

    fun presentNotification(activity: Activity, notification: NotificareNotification) {
        val type = NotificareNotification.NotificationType.from(notification.type) ?: run {
            NotificareLogger.warning("Trying to present a notification with an unknown type '${notification.type}'.")
            return
        }

        NotificareLogger.debug("Presenting notification '${notification.id}'.")

        when (type) {
            NotificareNotification.NotificationType.NONE -> {
                NotificareLogger.debug("Attempting to present a notification of type 'none'. These should be handled by the application instead.")
            }
            NotificareNotification.NotificationType.URL_SCHEME -> presentUrlScheme(activity, notification)
            else -> {
                val intent = Intent(Notificare.requireContext(), notificationActivity)
                    .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                    .setPackage(Notificare.requireContext().packageName)

                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    fun presentAction(activity: Activity, notification: NotificareNotification, action: NotificareNotification.Action) {
        NotificareLogger.debug("Presenting notification action '${action.type}' for notification '${notification.id}'.")

        GlobalScope.launch(Dispatchers.IO) {
            try {
                // NotificarePushUI.shared.delegate?.notificare(NotificarePushUI.shared, willExecuteAction: action, for: notification)

                if (action.type == NotificareNotification.Action.TYPE_CALLBACK && (action.camera || action.keyboard)) {
                    val intent = Intent(Notificare.requireContext(), notificationActivity)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                        .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                        .setPackage(Notificare.requireContext().packageName)

                    activity.startActivity(intent)
                    activity.overridePendingTransition(0, 0)

                    return@launch
                }

                val handler = createActionHandler(activity, notification, action) ?: run {
                    NotificareLogger.debug("Unable to create an action handler for '${action.type}'.")
                    return@launch
                }

                handler.execute()
            } catch (e: Exception) {
                // TODO
            }
        }
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
            NotificareNotification.NotificationType.WEB_VIEW -> NotificareWebViewFragment::class.java.canonicalName
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

    private fun presentUrlScheme(activity: Activity, notification: NotificareNotification) {
        val content = notification.content.firstOrNull { it.type == "re.notifica.content.URL" } ?: return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.data as String)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            setPackage(activity.applicationContext.applicationContext.packageName)
        }

        // Check if the application can handle the intent itself.
        if (intent.resolveActivity(activity.applicationContext.packageManager) != null) {
            activity.startActivity(intent)
        } else {
            intent.setPackage(null)
            if (intent.resolveActivity(activity.applicationContext.packageManager) != null) {
                activity.startActivity(intent)
            }
        }
    }

    internal fun createActionHandler(
        activity: Activity,
        notification: NotificareNotification,
        action: NotificareNotification.Action
    ): NotificationAction? {
        return when (action.type) {
            NotificareNotification.Action.TYPE_APP -> NotificationAppAction(activity, notification, action)
            NotificareNotification.Action.TYPE_BROWSER -> NotificationBrowserAction(activity, notification, action)
            NotificareNotification.Action.TYPE_CALLBACK -> NotificationCallbackAction(activity, notification, action)
            NotificareNotification.Action.TYPE_CUSTOM -> NotificationCustomAction(activity, notification, action)
            NotificareNotification.Action.TYPE_MAIL -> NotificationMailAction(activity, notification, action)
            NotificareNotification.Action.TYPE_SMS -> NotificationSmsAction(activity, notification, action)
            NotificareNotification.Action.TYPE_TELEPHONE -> NotificationTelephoneAction(activity, notification, action)
            NotificareNotification.Action.TYPE_WEB_VIEW -> TODO()
            else -> {
                NotificareLogger.warning("Unhandled action type '${action.type}'.")
                null
            }
        }
    }
}
