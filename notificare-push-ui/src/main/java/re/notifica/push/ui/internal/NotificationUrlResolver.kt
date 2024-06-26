package re.notifica.push.ui.internal

import android.net.Uri
import re.notifica.models.NotificareNotification

internal object NotificationUrlResolver {

    internal fun resolve(notification: NotificareNotification): UrlResolverResult {
        val content = notification.content.firstOrNull { it.type == "re.notifica.content.URL" }
            ?: return UrlResolverResult.NONE

        val url = (content.data as? String)?.let { Uri.parse(it) }
            ?: return UrlResolverResult.NONE

        val isHttpUrl = url.scheme == "http" || url.scheme == "https"
        val isDynamicLink = url.host?.endsWith("ntc.re") == true

        if (!isHttpUrl || isDynamicLink) return UrlResolverResult.URL_SCHEME

        val webViewQueryParameter = url.getQueryParameter("notificareWebView")
        val isWebViewMode = webViewQueryParameter == "1" || webViewQueryParameter?.lowercase() == "true"

        return if (isWebViewMode) UrlResolverResult.WEB_VIEW else UrlResolverResult.IN_APP_BROWSER
    }

    internal enum class UrlResolverResult {
        NONE,
        URL_SCHEME,
        IN_APP_BROWSER,
        WEB_VIEW,
    }
}