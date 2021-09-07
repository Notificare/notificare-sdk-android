package re.notifica.push.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.modules.NotificareModule
import re.notifica.push.ui.actions.*
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.notifications.fragments.*

public object NotificarePushUI : NotificareModule() {

    public const val SDK_VERSION: String = BuildConfig.SDK_VERSION

    private const val CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX = ".notificare.fileprovider"

    internal val contentFileProviderAuthority: String
        get() = "${Notificare.requireContext().packageName}$CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX"

    private var serviceManager: NotificareServiceManager? = null

    public var notificationActivity: Class<out NotificationActivity> = NotificationActivity::class.java

    private val _lifecycleListeners = mutableListOf<NotificationLifecycleListener>()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val lifecycleListeners: List<NotificationLifecycleListener> = _lifecycleListeners

    // region Notificare Module

    override fun configure() {
        serviceManager = NotificareServiceManager.Factory.create(Notificare.requireContext())
    }

    override suspend fun launch() {}

    override suspend fun unlaunch() {}

    // endregion

    public fun addLifecycleListener(listener: NotificationLifecycleListener) {
        _lifecycleListeners.add(listener)
    }

    public fun removeLifecycleListener(listener: NotificationLifecycleListener) {
        _lifecycleListeners.remove(listener)
    }

    public fun presentNotification(activity: Activity, notification: NotificareNotification) {
        val type = NotificareNotification.NotificationType.from(notification.type) ?: run {
            NotificareLogger.warning("Trying to present a notification with an unknown type '${notification.type}'.")
            return
        }

        NotificareLogger.debug("Presenting notification '${notification.id}'.")

        when (type) {
            NotificareNotification.NotificationType.NONE -> {
                NotificareLogger.debug("Attempting to present a notification of type 'none'. These should be handled by the application instead.")
            }
            NotificareNotification.NotificationType.URL_SCHEME -> {
                lifecycleListeners.forEach { it.onNotificationWillPresent(notification) }
                presentUrlScheme(activity, notification)
            }
            else -> {
                lifecycleListeners.forEach { it.onNotificationWillPresent(notification) }

                val intent = Intent(Notificare.requireContext(), notificationActivity)
                    .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                    .setPackage(Notificare.requireContext().packageName)

                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    public fun presentAction(
        activity: Activity,
        notification: NotificareNotification,
        action: NotificareNotification.Action
    ) {
        NotificareLogger.debug("Presenting notification action '${action.type}' for notification '${notification.id}'.")

        GlobalScope.launch(Dispatchers.IO) {
            try {
                lifecycleListeners.forEach { it.onActionWillExecute(notification, action) }

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

                    val error = Exception("Unable to create an action handler for '${action.type}'.")
                    lifecycleListeners.forEach { it.onActionFailedToExecute(notification, action, error) }

                    return@launch
                }

                handler.execute()
            } catch (e: Exception) {
                lifecycleListeners.forEach { it.onActionFailedToExecute(notification, action, e) }
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
            NotificareNotification.NotificationType.IMAGE -> NotificareImageFragment::class.java.canonicalName
            NotificareNotification.NotificationType.PASSBOOK -> {
                // TODO: handle passbook notification
                return null
            }
            NotificareNotification.NotificationType.VIDEO -> NotificareVideoFragment::class.java.canonicalName
            NotificareNotification.NotificationType.MAP,
            NotificareNotification.NotificationType.RATE,
            NotificareNotification.NotificationType.STORE -> {
                val manager = serviceManager ?: run {
                    NotificareLogger.warning("No push-ui dependencies have been detected. Please include one of the platform-specific push-ui packages.")
                    return null
                }

                return manager.getFragmentCanonicalClassName(notification)
            }
        }
    }

    private fun presentUrlScheme(activity: Activity, notification: NotificareNotification) {
        val content = notification.content.firstOrNull { it.type == "re.notifica.content.URL" } ?: run {
            lifecycleListeners.forEach { it.onNotificationFailedToPresent(notification) }
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.data as String)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            setPackage(activity.applicationContext.packageName)
        }

        // Check if the application can handle the intent itself.
        if (intent.resolveActivity(activity.applicationContext.packageManager) != null) {
            activity.startActivity(intent)
            lifecycleListeners.forEach { it.onNotificationPresented(notification) }
        } else {
            NotificareLogger.warning("Cannot open a deep link that's not supported by the application.")
            lifecycleListeners.forEach { it.onNotificationFailedToPresent(notification) }
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
            NotificareNotification.Action.TYPE_WEB_VIEW -> NotificationWebViewAction(activity, notification, action)
            else -> {
                NotificareLogger.warning("Unhandled action type '${action.type}'.")
                null
            }
        }
    }

    public interface NotificationLifecycleListener {
        public fun onNotificationWillPresent(notification: NotificareNotification) {
            NotificareLogger.debug("Notification will present, please override onNotificationPresented if you want to receive these events.")
        }

        public fun onNotificationPresented(notification: NotificareNotification) {
            NotificareLogger.debug("Notification presented, please override onNotificationPresented if you want to receive these events.")
        }

        public fun onNotificationFinishedPresenting(notification: NotificareNotification) {
            NotificareLogger.debug("Notification finished presenting, please override onNotificationFinishedPresenting if you want to receive these events.")
        }

        public fun onNotificationFailedToPresent(notification: NotificareNotification) {
            NotificareLogger.debug("Notification failed to present, please override onNotificationFailedToPresent if you want to receive these events.")
        }

        public fun onNotificationUrlClicked(notification: NotificareNotification, uri: Uri) {
            NotificareLogger.debug("Notification url clicked, please override onNotificationUrlClicked if you want to receive these events.")
        }

        public fun onActionWillExecute(notification: NotificareNotification, action: NotificareNotification.Action) {
            NotificareLogger.debug("Action will execute, please override onActionWillExecute if you want to receive these events.")
        }

        public fun onActionExecuted(notification: NotificareNotification, action: NotificareNotification.Action) {
            NotificareLogger.debug("Action executed, please override onActionExecuted if you want to receive these events.")
        }

//        fun onActionNotExecuted(notification: NotificareNotification, action: NotificareNotification.Action) {
//            NotificareLogger.debug("Action did not execute, please override onActionNotExecuted if you want to receive these events.")
//        }

        public fun onActionFailedToExecute(
            notification: NotificareNotification,
            action: NotificareNotification.Action,
            error: Exception?
        ) {
            NotificareLogger.debug(
                "Action failed to execute, please override onActionFailedToExecute if you want to receive these events.",
                error
            )
        }

        public fun onCustomActionReceived(
            notification: NotificareNotification,
            action: NotificareNotification.Action,
            uri: Uri
        ) {
            NotificareLogger.info("Action received, please override onCustomActionReceived if you want to receive these events.")
        }
    }
}
