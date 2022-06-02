package re.notifica.push.ui.internal

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import kotlinx.coroutines.*
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.*
import re.notifica.push.ui.actions.*
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.ktx.loyaltyIntegration
import re.notifica.push.ui.notifications.fragments.*

internal object NotificarePushUIImpl : NotificareModule(), NotificarePushUI, NotificareInternalPushUI {

    private const val CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX = ".notificare.fileprovider"

    internal val contentFileProviderAuthority: String
        get() = "${Notificare.requireContext().packageName}$CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX"

    private var serviceManager: ServiceManager? = null
    private val _lifecycleListeners = mutableListOf<NotificarePushUI.NotificationLifecycleListener>()


    // region Notificare Module

    override fun configure() {
        serviceManager = ServiceManager.create()
    }

    // endregion

    // region Notificare Push UI

    override var notificationActivity: Class<out NotificationActivity> = NotificationActivity::class.java

    override fun addLifecycleListener(listener: NotificarePushUI.NotificationLifecycleListener) {
        _lifecycleListeners.add(listener)
    }

    override fun removeLifecycleListener(listener: NotificarePushUI.NotificationLifecycleListener) {
        _lifecycleListeners.remove(listener)
    }

    override fun presentNotification(activity: Activity, notification: NotificareNotification) {
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
                handleUrlScheme(activity, notification)
            }
            NotificareNotification.NotificationType.PASSBOOK -> {
                lifecycleListeners.forEach { it.onNotificationWillPresent(notification) }
                handlePassbook(activity, notification)
            }
            else -> {
                lifecycleListeners.forEach { it.onNotificationWillPresent(notification) }
                openNotificationActivity(activity, notification)
            }
        }
    }

    override fun presentAction(
        activity: Activity,
        notification: NotificareNotification,
        action: NotificareNotification.Action
    ) {
        NotificareLogger.debug("Presenting notification action '${action.type}' for notification '${notification.id}'.")

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    lifecycleListeners.forEach { it.onActionWillExecute(notification, action) }
                }

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

                    withContext(Dispatchers.Main) {
                        val error = Exception("Unable to create an action handler for '${action.type}'.")
                        lifecycleListeners.forEach { it.onActionFailedToExecute(notification, action, error) }
                    }

                    return@launch
                }

                handler.execute()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lifecycleListeners.forEach { it.onActionFailedToExecute(notification, action, e) }
                }
            }
        }
    }

    // endregion

    // region Notificare Internal Push UI

    override val lifecycleListeners: List<NotificarePushUI.NotificationLifecycleListener> = _lifecycleListeners

    // endregion

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
            NotificareNotification.NotificationType.PASSBOOK -> NotificareWebPassFragment::class.java.canonicalName
            NotificareNotification.NotificationType.VIDEO -> NotificareVideoFragment::class.java.canonicalName
            NotificareNotification.NotificationType.MAP,
            NotificareNotification.NotificationType.RATE,
            NotificareNotification.NotificationType.STORE -> {
                val manager = serviceManager ?: run {
                    NotificareLogger.warning("No push-ui dependencies have been detected. Please include one of the platform-specific push-ui packages.")
                    return null
                }

                return manager.getFragmentClass(notification).canonicalName
            }
        }
    }

    private fun handleUrlScheme(activity: Activity, notification: NotificareNotification) {
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

    private fun handlePassbook(activity: Activity, notification: NotificareNotification) {
        val integration = Notificare.loyaltyIntegration() ?: run {
            openNotificationActivity(activity, notification)
            return
        }

        integration.handlePassPresentation(activity, notification, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                lifecycleListeners.forEach { it.onNotificationPresented(notification) }
            }

            override fun onFailure(e: Exception) {
                lifecycleListeners.forEach { it.onNotificationFailedToPresent(notification) }
            }
        })
    }

    private fun openNotificationActivity(
        activity: Activity,
        notification: NotificareNotification,
        extras: Bundle = bundleOf(),
    ) {
        val intent = Intent(Notificare.requireContext(), notificationActivity)
            .putExtras(extras)
            .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
            .setPackage(Notificare.requireContext().packageName)

        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }

    internal fun createActionHandler(
        activity: Activity,
        notification: NotificareNotification,
        action: NotificareNotification.Action,
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

    internal fun createInAppBrowser(): CustomTabsIntent {
        val colorScheme = when (Notificare.options?.customTabsColorScheme) {
            "light" -> CustomTabsIntent.COLOR_SCHEME_LIGHT
            "dark" -> CustomTabsIntent.COLOR_SCHEME_DARK
            else -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
        }

        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
            .apply {
                val toolbarColor = Notificare.options?.customTabsToolbarColor
                if (toolbarColor != null) setToolbarColor(toolbarColor)

                val navigationBarColor = Notificare.options?.customTabsNavigationBarColor
                if (navigationBarColor != null) setNavigationBarColor(navigationBarColor)

                val navigationBarDividerColor = Notificare.options?.customTabsNavigationBarDividerColor
                if (navigationBarDividerColor != null) setNavigationBarDividerColor(navigationBarDividerColor)
            }
            .build()

        return CustomTabsIntent.Builder()
            .setShowTitle(checkNotNull(Notificare.options).customTabsShowTitle)
            .setColorScheme(colorScheme)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, colorSchemeParams)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, colorSchemeParams)
            .build()
    }
}
