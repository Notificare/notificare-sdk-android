package re.notifica.push.ui.utils

import android.annotation.TargetApi
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.fragments.NotificationFragment

open class NotificationWebViewClient(
    private val notification: NotificareNotification,
    private val callback: NotificationFragment.Callback,
) : WebViewClient() {

    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return super.shouldOverrideUrlLoading(view, request)
        // return handleOpenActions(request.getUrl()) || handleOpenAction(request.getUrl()) || handleUri(view, request.getUrl());
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return super.shouldOverrideUrlLoading(view, url)
        // Uri uri = Uri.parse(url);
        // return handleOpenActions(uri) || handleOpenAction(uri) || handleUri(view, uri);
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
    }
}
