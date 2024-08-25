package re.notifica.push.ui.notifications.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import re.notifica.Notificare
import re.notifica.utilities.onMainThread
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.databinding.NotificareNotificationWebPassFragmentBinding
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment
import re.notifica.push.ui.utils.NotificationWebViewClient

public class NotificareWebPassFragment : NotificationFragment() {

    private lateinit var binding: NotificareNotificationWebPassFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NotificareNotificationWebPassFragmentBinding.inflate(inflater, container, false)
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
        val content = notification.content.firstOrNull()
        val passUrlStr = content?.data as? String
        val application = Notificare.application
        val host = Notificare.servicesInfo?.hosts?.restApi

        if (
            content?.type != NotificareNotification.Content.TYPE_PK_PASS ||
            passUrlStr == null ||
            application == null ||
            host == null
        ) {
            onMainThread {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.get()?.onNotificationFailedToPresent(notification)
                }
            }

            return
        }

        val components = passUrlStr.split("/")
        val id = components.last()

        val url = "$host/pass/web/$id?showWebVersion=1"

        binding.webView.loadUrl(url)
    }
}
