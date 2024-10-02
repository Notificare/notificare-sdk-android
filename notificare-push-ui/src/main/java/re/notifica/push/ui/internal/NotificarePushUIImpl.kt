package re.notifica.push.ui.internal

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Keep
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.internal.NotificareModule
import re.notifica.utilities.threading.onMainThread
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificareInternalPushUI
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.NotificationActivity
import re.notifica.push.ui.actions.NotificationAppAction
import re.notifica.push.ui.actions.NotificationBrowserAction
import re.notifica.push.ui.actions.NotificationCallbackAction
import re.notifica.push.ui.actions.NotificationCustomAction
import re.notifica.push.ui.actions.NotificationInAppBrowserAction
import re.notifica.push.ui.actions.NotificationMailAction
import re.notifica.push.ui.actions.NotificationSmsAction
import re.notifica.push.ui.actions.NotificationTelephoneAction
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.customTabsColorScheme
import re.notifica.push.ui.customTabsNavigationBarColor
import re.notifica.push.ui.customTabsNavigationBarDividerColor
import re.notifica.push.ui.customTabsShowTitle
import re.notifica.push.ui.customTabsToolbarColor
import re.notifica.push.ui.ktx.loyaltyIntegration
import re.notifica.push.ui.notifications.fragments.NotificareAlertFragment
import re.notifica.push.ui.notifications.fragments.NotificareImageFragment
import re.notifica.push.ui.notifications.fragments.NotificareMapFragment
import re.notifica.push.ui.notifications.fragments.NotificareRateFragment
import re.notifica.push.ui.notifications.fragments.NotificareStoreFragment
import re.notifica.push.ui.notifications.fragments.NotificareUrlFragment
import re.notifica.push.ui.notifications.fragments.NotificareVideoFragment
import re.notifica.push.ui.notifications.fragments.NotificareWebPassFragment
import re.notifica.push.ui.notifications.fragments.NotificareWebViewFragment
import re.notifica.push.ui.utils.removeQueryParameter
import re.notifica.utilities.coroutines.notificareCoroutineScope
import java.lang.ref.WeakReference

@Keep
internal object NotificarePushUIImpl : NotificareModule(), NotificarePushUI, NotificareInternalPushUI {

    private const val CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX = ".notificare.fileprovider"

    internal val contentFileProviderAuthority: String
        get() = "${Notificare.requireContext().packageName}$CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX"

    private val _lifecycleListeners = mutableListOf<WeakReference<NotificarePushUI.NotificationLifecycleListener>>()

    override fun configure() {
        logger.hasDebugLoggingEnabled = checkNotNull(Notificare.options).debugLoggingEnabled
    }

    // region Notificare Push UI

    override var notificationActivity: Class<out NotificationActivity> = NotificationActivity::class.java

    override fun addLifecycleListener(listener: NotificarePushUI.NotificationLifecycleListener) {
        _lifecycleListeners.add(WeakReference(listener))
    }

    override fun removeLifecycleListener(listener: NotificarePushUI.NotificationLifecycleListener) {
        val iterator = _lifecycleListeners.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next().get()
            if (next == null || next == listener) {
                iterator.remove()
            }
        }
    }

    override fun presentNotification(activity: Activity, notification: NotificareNotification) {
        val type = NotificareNotification.NotificationType.from(notification.type) ?: run {
            logger.warning("Trying to present a notification with an unknown type '${notification.type}'.")
            return
        }

        logger.debug("Presenting notification '${notification.id}'.")

        when (type) {
            NotificareNotification.NotificationType.NONE -> {
                logger.debug(
                    "Attempting to present a notification of type 'none'. These should be handled by the application instead."
                )
            }
            NotificareNotification.NotificationType.URL_SCHEME -> {
                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onNotificationWillPresent(notification) }
                }

                handleUrlScheme(activity, notification)
            }
            NotificareNotification.NotificationType.URL_RESOLVER -> {
                handleUrlResolver(activity, notification)
            }
            NotificareNotification.NotificationType.PASSBOOK -> {
                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onNotificationWillPresent(notification) }
                }

                handlePassbook(activity, notification)
            }
            NotificareNotification.NotificationType.IN_APP_BROWSER -> {
                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onNotificationWillPresent(notification) }
                }

                handleInAppBrowser(activity, notification)
            }
            else -> {
                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onNotificationWillPresent(notification) }
                }

                openNotificationActivity(activity, notification)
            }
        }
    }

    override fun presentAction(
        activity: Activity,
        notification: NotificareNotification,
        action: NotificareNotification.Action
    ) {
        logger.debug("Presenting notification action '${action.type}' for notification '${notification.id}'.")

        notificareCoroutineScope.launch {
            try {
                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onActionWillExecute(notification, action) }
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
                    logger.debug("Unable to create an action handler for '${action.type}'.")

                    onMainThread {
                        val error = Exception("Unable to create an action handler for '${action.type}'.")
                        lifecycleListeners.forEach { it.get()?.onActionFailedToExecute(notification, action, error) }
                    }

                    return@launch
                }

                handler.execute()
            } catch (e: Exception) {
                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onActionFailedToExecute(notification, action, e) }
                }
            }
        }
    }

    // endregion

    // region Notificare Internal Push UI

    override val lifecycleListeners: List<WeakReference<NotificarePushUI.NotificationLifecycleListener>> = _lifecycleListeners

    // endregion

    internal fun getFragmentCanonicalClassName(notification: NotificareNotification): String? {
        val type = NotificareNotification.NotificationType.from(notification.type) ?: run {
            logger.warning("Unhandled notification type '${notification.type}'.")
            return null
        }

        return when (type) {
            NotificareNotification.NotificationType.NONE -> {
                logger.debug(
                    "Attempting to create a fragment for a notification of type 'none'. This type contains to visual interface."
                )
                return null
            }
            NotificareNotification.NotificationType.ALERT -> NotificareAlertFragment::class.java.canonicalName
            NotificareNotification.NotificationType.IN_APP_BROWSER -> {
                logger.debug(
                    "Attempting to create a fragment for a notification of type 'InAppBrowser'. This type contains no visual interface."
                )
                return null
            }
            NotificareNotification.NotificationType.WEB_VIEW -> NotificareWebViewFragment::class.java.canonicalName
            NotificareNotification.NotificationType.URL -> NotificareUrlFragment::class.java.canonicalName
            NotificareNotification.NotificationType.URL_RESOLVER -> NotificareUrlFragment::class.java.canonicalName
            NotificareNotification.NotificationType.URL_SCHEME -> {
                logger.debug(
                    "Attempting to create a fragment for a notification of type 'UrlScheme'. This type contains no visual interface."
                )
                return null
            }
            NotificareNotification.NotificationType.IMAGE -> NotificareImageFragment::class.java.canonicalName
            NotificareNotification.NotificationType.PASSBOOK -> NotificareWebPassFragment::class.java.canonicalName
            NotificareNotification.NotificationType.VIDEO -> NotificareVideoFragment::class.java.canonicalName
            NotificareNotification.NotificationType.MAP -> NotificareMapFragment::class.java.canonicalName
            NotificareNotification.NotificationType.RATE -> NotificareRateFragment::class.java.canonicalName
            NotificareNotification.NotificationType.STORE -> NotificareStoreFragment::class.java.canonicalName
        }
    }

    private fun handleUrlScheme(activity: Activity, notification: NotificareNotification) {
        val servicesInfo = Notificare.servicesInfo ?: run {
            logger.warning("Notificare is not configured.")

            onMainThread {
                lifecycleListeners.forEach { it.get()?.onNotificationFailedToPresent(notification) }
            }

            return
        }

        val content = notification.content.firstOrNull { it.type == "re.notifica.content.URL" } ?: run {
            onMainThread {
                lifecycleListeners.forEach { it.get()?.onNotificationFailedToPresent(notification) }
            }

            return
        }

        val url = Uri.parse(content.data as String)
        if (url.host?.endsWith(servicesInfo.hosts.shortLinks) != true) {
            presentDeepLink(activity, notification, url)
            return
        }

        notificareCoroutineScope.launch {
            try {
                val link = Notificare.fetchDynamicLink(url)
                presentDeepLink(activity, notification, Uri.parse(link.target))
            } catch (e: Exception) {
                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onNotificationFailedToPresent(notification) }
                }
            }
        }
    }

    private fun presentDeepLink(activity: Activity, notification: NotificareNotification, url: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, url).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            setPackage(activity.applicationContext.packageName)
        }

        // Check if the application can handle the intent itself.
        if (intent.resolveActivity(activity.applicationContext.packageManager) == null) {
            logger.warning("Cannot open a deep link that's not supported by the application.")

            onMainThread {
                lifecycleListeners.forEach { it.get()?.onNotificationFailedToPresent(notification) }
            }

            return
        }

        activity.startActivity(intent)

        onMainThread {
            lifecycleListeners.forEach { it.get()?.onNotificationPresented(notification) }
        }
    }

    private fun handleUrlResolver(activity: Activity, notification: NotificareNotification) {
        val servicesInfo = Notificare.servicesInfo ?: run {
            logger.warning("Notificare is not configured.")

            onMainThread {
                lifecycleListeners.forEach { it.get()?.onNotificationFailedToPresent(notification) }
            }

            return
        }

        val result = NotificationUrlResolver.resolve(notification, servicesInfo)

        when (result) {
            NotificationUrlResolver.UrlResolverResult.NONE -> {
                logger.debug("Resolving as 'none' notification.")
            }
            NotificationUrlResolver.UrlResolverResult.URL_SCHEME -> {
                logger.debug("Resolving as 'url scheme' notification.")

                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onNotificationWillPresent(notification) }
                }

                handleUrlScheme(activity, notification)
            }
            NotificationUrlResolver.UrlResolverResult.WEB_VIEW -> {
                logger.debug("Resolving as 'web view' notification.")

                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onNotificationWillPresent(notification) }
                }

                openNotificationActivity(activity, notification)
            }
            NotificationUrlResolver.UrlResolverResult.IN_APP_BROWSER -> {
                logger.debug("Resolving as 'in-app browser' notification.")

                onMainThread {
                    lifecycleListeners.forEach { it.get()?.onNotificationWillPresent(notification) }
                }

                handleInAppBrowser(activity, notification)
            }
        }
    }

    private fun handlePassbook(activity: Activity, notification: NotificareNotification) {
        val integration = Notificare.loyaltyIntegration() ?: run {
            openNotificationActivity(activity, notification)
            return
        }

        integration.handlePassPresentation(
            activity,
            notification,
            object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    onMainThread {
                        lifecycleListeners.forEach { it.get()?.onNotificationPresented(notification) }
                    }
                }

                override fun onFailure(e: Exception) {
                    onMainThread {
                        lifecycleListeners.forEach { it.get()?.onNotificationFailedToPresent(notification) }
                    }
                }
            }
        )
    }

    private fun handleInAppBrowser(activity: Activity, notification: NotificareNotification) {
        val content = notification.content.firstOrNull { it.type == "re.notifica.content.URL" }
        val urlStr = content?.data as? String ?: run {
            onMainThread {
                lifecycleListeners.forEach { it.get()?.onNotificationFailedToPresent(notification) }
            }

            return
        }

        val url = Uri.parse(urlStr)
            .buildUpon()
            .removeQueryParameter("notificareWebView")
            .build()

        try {
            createInAppBrowser().launchUrl(activity, url)

            onMainThread {
                lifecycleListeners.forEach { it.get()?.onNotificationPresented(notification) }
            }
        } catch (e: Exception) {
            logger.error("Failed launch in-app browser.", e)

            onMainThread {
                lifecycleListeners.forEach { it.get()?.onNotificationFailedToPresent(notification) }
            }
        }
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
            @Suppress("DEPRECATION", "ktlint:standard:annotation")
            NotificareNotification.Action.TYPE_WEB_VIEW,
            NotificareNotification.Action.TYPE_IN_APP_BROWSER ->
                NotificationInAppBrowserAction(activity, notification, action)
            else -> {
                logger.warning("Unhandled action type '${action.type}'.")
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
