package re.notifica.push.ui.utils

import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.closeWindowQueryParameter
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment
import re.notifica.push.ui.openActionQueryParameter
import re.notifica.push.ui.openActionsQueryParameter
import re.notifica.push.ui.urlSchemes

open class NotificationWebViewClient(
    private val notification: NotificareNotification,
    private val callback: NotificationFragment.Callback,
) : WebViewClient() {

    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return handleOpenActions(request.url) || handleOpenAction(request.url) || handleUri(view, request.url)
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        val uri = Uri.parse(url)
        return handleOpenActions(uri) || handleOpenAction(uri) || handleUri(view, uri)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        view.evaluateJavascript("(function() { return (document.body.innerHTML); })();") { html ->
            val openActionQueryParameter = checkNotNull(Notificare.options).openActionQueryParameter
            val openActionsQueryParameter = checkNotNull(Notificare.options).openActionsQueryParameter

            if (html != null && (html.contains(openActionQueryParameter) || html.contains(openActionsQueryParameter))) {
                callback.onNotificationFragmentCanHideActionsMenu()
            }
        }
    }

    private fun shouldCloseWindow(uri: Uri?): Boolean {
        if (uri != null && uri.isHierarchical) {
            val closeWindowParameter = uri.getQueryParameter(checkNotNull(Notificare.options).closeWindowQueryParameter)
            return closeWindowParameter != null && (closeWindowParameter == "1" || closeWindowParameter == "true")
        }

        return false
    }

    private fun handleOpenActions(uri: Uri?): Boolean {
        if (uri != null && uri.isHierarchical) {
            val openActionsWindowParameter = uri.getQueryParameter(
                checkNotNull(Notificare.options).openActionsQueryParameter
            )

            if (openActionsWindowParameter != null && (openActionsWindowParameter == "1" || openActionsWindowParameter == "true")) {
                callback.onNotificationFragmentShowActions()
                return true
            }
        }

        return false
    }

    private fun handleOpenAction(uri: Uri?): Boolean {
        if (uri != null && uri.isHierarchical) {
            val openActionWindowParameter = uri.getQueryParameter(
                checkNotNull(Notificare.options).openActionQueryParameter
            )

            if (openActionWindowParameter != null) {
                val action = notification.actions.firstOrNull { it.label == openActionWindowParameter }
                if (action != null) {
                    callback.onNotificationFragmentHandleAction(action)
                    return true
                }
            }
        }

        return false
    }

    private fun handleUri(view: WebView, uri: Uri?): Boolean {
        if (uri == null || uri.scheme == null) {
            // Relative URL
            if (shouldCloseWindow(uri)) {
                callback.onNotificationFragmentFinished()
            }

            return false
        }

        val options = checkNotNull(Notificare.options)
        if (options.urlSchemes.contains(uri.scheme)) {
            // TODO should become a new intent receiver
            val broadcast = Intent(Notificare.requireContext(), Notificare.intentReceiver).apply {
                data = uri
                action = "Notificare.INTENT_ACTION_URL_CLICKED" // TODO update to constant
                putExtra("Notificare.INTENT_EXTRA_NOTIFICATION", notification)
            }

            Notificare.requireContext().sendBroadcast(broadcast)

            if (shouldCloseWindow(uri)) {
                callback.onNotificationFragmentFinished()
            }

            return true
        } else if (uri.scheme == "http" || uri.scheme == "https") {
            // Normal HTTP links will be handled by the webView itself, unless they match any of the app's intent filters
            val httpIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(Notificare.requireContext().packageName)
            }

            if (httpIntent.resolveActivity(Notificare.requireContext().packageManager) != null) {
                // current application context can handle the intent itself
                callback.onNotificationFragmentStartActivity(httpIntent)

                if (shouldCloseWindow(uri)) {
                    callback.onNotificationFragmentFinished()
                }

                return true
            }

            // let other http links be handled by the webView itself
            return false
        } else {
            val uriIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(Notificare.requireContext().packageName)
            }

            if (uriIntent.resolveActivity(Notificare.requireContext().packageManager) != null) {
                // current application context can handle the intent itself
                callback.onNotificationFragmentStartActivity(uriIntent)
            } else {
                try {
                    // see if there is an application that can handle this intent
                    uriIntent.setPackage(null)

                    callback.onNotificationFragmentStartActivity(uriIntent)
                } catch (e: ActivityNotFoundException) {
                    NotificareLogger.warning("Could not find an activity capable of opening the URL.", e)
                }
            }

            if (shouldCloseWindow(uri)) {
                callback.onNotificationFragmentFinished()
            }

            return true
        }
    }
}
