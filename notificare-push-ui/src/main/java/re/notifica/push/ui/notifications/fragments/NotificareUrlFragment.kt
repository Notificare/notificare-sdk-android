package re.notifica.push.ui.notifications.fragments

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import re.notifica.Notificare
import re.notifica.utilities.threading.onMainThread
import re.notifica.push.ui.databinding.NotificareNotificationUrlFragmentBinding
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment
import re.notifica.push.ui.utils.NotificationWebViewClient
import re.notifica.push.ui.utils.removeQueryParameter

public class NotificareUrlFragment : NotificationFragment() {

    private lateinit var binding: NotificareNotificationUrlFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NotificareNotificationUrlFragmentBinding.inflate(inflater, container, false)
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
        val urlStr = content?.data as? String ?: run {
            onMainThread {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.get()?.onNotificationFailedToPresent(notification)
                }
            }

            return
        }

        val url = Uri.parse(urlStr)
            .buildUpon()
            .removeQueryParameter("notificareWebView")
            .build()
            .toString()

        binding.webView.loadUrl(url)
    }
}
