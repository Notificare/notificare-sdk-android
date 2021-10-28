package re.notifica.push.ui.notifications.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.webkit.WebChromeClient
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificationActivity
import re.notifica.push.ui.R
import re.notifica.push.ui.databinding.NotificareNotificationWebPassFragmentBinding
import re.notifica.push.ui.ktx.loyaltyIntegration
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment
import re.notifica.push.ui.utils.NotificationWebViewClient

public class NotificareWebPassFragment : NotificationFragment() {

    private lateinit var binding: NotificareNotificationWebPassFragmentBinding
    private var passIsPresentInWallet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Notificare.loyaltyIntegration() != null) {
            // When the Loyalty module is available, we have to show the add/remove to wallet buttons.
            setHasOptionsMenu(true)
        }

        passIsPresentInWallet =
            savedInstanceState?.getBoolean(NotificationActivity.INTENT_EXTRA_PASSBOOK_IN_WALLET, false)
                ?: activity?.intent?.getBooleanExtra(NotificationActivity.INTENT_EXTRA_PASSBOOK_IN_WALLET, false)
                    ?: false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (Notificare.loyaltyIntegration() != null) {
            inflater.inflate(R.menu.notificare_menu_notification_web_pass_fragment, menu)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.notificare_action_add_pass_to_wallet)?.apply {
            isVisible = if (Notificare.loyaltyIntegration() != null) {
                !passIsPresentInWallet
            } else {
                false
            }
        }

        menu.findItem(R.id.notificare_action_remove_pass_from_wallet)?.apply {
            isVisible = if (Notificare.loyaltyIntegration() != null) {
                passIsPresentInWallet
            } else {
                false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.notificare_action_add_pass_to_wallet -> {
                Notificare.loyaltyIntegration()?.handleStorageUpdate(
                    notification = notification,
                    includeInWallet = true,
                    callback = object : NotificareCallback<Unit> {
                        override fun onSuccess(result: Unit) {

                        }

                        override fun onFailure(e: Exception) {

                        }
                    }
                )

                return true
            }
            R.id.notificare_action_remove_pass_from_wallet -> {
                Notificare.loyaltyIntegration()?.handleStorageUpdate(
                    notification = notification,
                    includeInWallet = false,
                    callback = object : NotificareCallback<Unit> {
                        override fun onSuccess(result: Unit) {

                        }

                        override fun onFailure(e: Exception) {

                        }
                    }
                )

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(NotificationActivity.INTENT_EXTRA_PASSBOOK_IN_WALLET, passIsPresentInWallet)
    }


    private fun setupContent() {
        val content = notification.content.firstOrNull()
        val passUrlStr = content?.data as? String
        val application = Notificare.application
        val host = Notificare.servicesInfo?.environment?.pushHost

        if (content?.type != NotificareNotification.Content.TYPE_PK_PASS || passUrlStr == null || application == null || host == null) {
            Notificare.pushUIInternal().lifecycleListeners.forEach { it.onNotificationFailedToPresent(notification) }
            return
        }

        val components = passUrlStr.split("/")
        val id = components.last()

        val url = "$host/pass/web/$id?showWebVersion=1"

        binding.webView.loadUrl(url)
    }
}
