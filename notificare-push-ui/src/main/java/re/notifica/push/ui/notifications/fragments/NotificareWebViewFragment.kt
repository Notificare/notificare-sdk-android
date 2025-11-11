package re.notifica.push.ui.notifications.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import re.notifica.Notificare
import re.notifica.utilities.threading.onMainThread
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.databinding.NotificareNotificationWebViewFragmentBinding
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment
import re.notifica.push.ui.utils.NotificationWebViewClient

public class NotificareWebViewFragment : NotificationFragment() {

    private lateinit var binding: NotificareNotificationWebViewFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NotificareNotificationWebViewFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure the WebView.
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.clearCache(true)
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = NotificationWebViewClient(notification, callback)

        setupContent()
    }

    private fun setupContent() {
        val content = notification.content.firstOrNull { it.type == NotificareNotification.Content.TYPE_HTML }
        val html = content?.data as? String ?: run {
            onMainThread {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.get()?.onNotificationFailedToPresent(notification)
                }
            }

            return
        }

        val referrer = context?.packageName?.let { "https://$it" }

        binding.webView.loadDataWithBaseURL(
            referrer ?: "x-data:/base",
            html,
            "text/html",
            "utf-8",
            null,
        )

        if (html.contains("getOpenActionQueryParameter") || html.contains("getOpenActionsQueryParameter")) {
            callback.onNotificationFragmentCanHideActionsMenu()
        }
    }
}
