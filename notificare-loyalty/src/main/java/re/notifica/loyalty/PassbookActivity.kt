package re.notifica.loyalty

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareUtils
import re.notifica.loyalty.ktx.INTENT_EXTRA_PASSBOOK
import re.notifica.loyalty.ktx.loyalty
import re.notifica.loyalty.ktx.loyaltyImplementation
import re.notifica.loyalty.models.NotificarePass

public open class PassbookActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var pass: NotificarePass? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notificare_passbook_activity)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configure the WebView.
        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.clearCache(true)
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        val pass = savedInstanceState?.getParcelable(Notificare.INTENT_EXTRA_PASSBOOK)
            ?: intent.getParcelableExtra<NotificarePass>(Notificare.INTENT_EXTRA_PASSBOOK)

        if (pass != null) {
            handlePass(pass)
            return
        }

        val serial = parsePassbookIntent(intent) ?: run {
            val error = IllegalArgumentException("Received an invalid URI: ${intent.data}")
            handlePassLoadingError(error)

            return
        }

        handlePassSerial(serial)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.notificare_menu_passbook_activity, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        pass.let { pass ->
            menu.findItem(R.id.notificare_action_add_pass_to_wallet)?.apply {
                isVisible = pass != null && pass.version == 1 && !Notificare.loyalty().isInWallet(pass)
            }

            menu.findItem(R.id.notificare_action_remove_pass_from_wallet)?.apply {
                isVisible = pass != null && pass.version == 1 && Notificare.loyalty().isInWallet(pass)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.notificare_action_add_pass_to_wallet -> {
                Notificare.loyalty().addPass(checkNotNull(pass), object : NotificareCallback<Unit> {
                    override fun onSuccess(result: Unit) {
                        finish()
                    }

                    override fun onFailure(e: Exception) {
                        AlertDialog.Builder(this@PassbookActivity)
                            .setTitle(NotificareUtils.applicationName)
                            .setMessage(R.string.notificare_passbook_error_adding_pass)
                            .setPositiveButton(R.string.notificare_dialog_ok_button) { _, _ -> finish() }
                            .show()
                    }
                })
                return true
            }
            R.id.notificare_action_remove_pass_from_wallet -> {
                Notificare.loyalty().removePass(checkNotNull(pass), object : NotificareCallback<Unit> {
                    override fun onSuccess(result: Unit) {
                        finish()
                    }

                    override fun onFailure(e: Exception) {
                        AlertDialog.Builder(this@PassbookActivity)
                            .setTitle(NotificareUtils.applicationName)
                            .setMessage(R.string.notificare_passbook_error_removing_pass)
                            .setPositiveButton(R.string.notificare_dialog_ok_button) { _, _ -> finish() }
                            .show()
                    }
                })
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pass?.let { outState.putParcelable(Notificare.INTENT_EXTRA_PASSBOOK, it) }
    }


    protected open fun handlePass(pass: NotificarePass) {
        this.pass = pass
        invalidateOptionsMenu()

        when (pass.version) {
            1 -> showWebPassView(pass.serial)
            2 -> {
                val url = pass.googlePaySaveLink ?: run {
                    val error = IllegalArgumentException("Pass v2 doesn't contain a Google Pay link.")
                    handlePassLoadingError(error)

                    return
                }

                showGooglePayView(url)
            }
            else -> {
                val error = IllegalArgumentException("Unsupported pass version: ${pass.version}")
                handlePassLoadingError(error)
            }
        }
    }

    protected open fun handlePassSerial(serial: String) {
        Notificare.loyaltyImplementation().fetchPassBySerial(serial, object : NotificareCallback<NotificarePass> {
            override fun onSuccess(result: NotificarePass) {
                handlePass(result)
            }

            override fun onFailure(e: Exception) {
                handlePassLoadingError(e)
            }
        })
    }

    protected open fun handlePassLoadingError(e: Exception) {
        AlertDialog.Builder(this)
            .setTitle(NotificareUtils.applicationName)
            .setMessage(R.string.notificare_passbook_error_loading_pass)
            .setPositiveButton(R.string.notificare_dialog_ok_button, null)
            .setOnDismissListener { finish() }
            .show()
    }

    private fun parsePassbookIntent(intent: Intent): String? {
        val uri = intent.data ?: return null
        val pathSegments = uri.pathSegments ?: return null

        val application = Notificare.application ?: return null
        val appLinksDomain = Notificare.servicesInfo?.appLinksDomain ?: return null

        if (uri.host == "${application.id}.${appLinksDomain}" && pathSegments.size >= 2 && pathSegments[0] == "pass") {
            return pathSegments[1]
        }

        return null
    }

    private fun showWebPassView(serial: String) {
        val host = Notificare.servicesInfo?.pushHost ?: return
        val url = "$host/pass/web/$serial?showWebVersion=1"

        webView.loadUrl(url)
    }

    private fun showGooglePayView(url: String) {
        try {
            val intent = Intent().setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(url))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            startActivity(intent)
            finish()
        } catch (e: ActivityNotFoundException) {
            NotificareLogger.error("Unable to show the Google Pay pass.", e)
            handlePassLoadingError(e)
        }
    }
}
