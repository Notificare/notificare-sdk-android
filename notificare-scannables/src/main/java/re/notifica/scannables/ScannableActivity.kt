package re.notifica.scannables

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import re.notifica.NotificareCallback
import re.notifica.NotificareLogger
import re.notifica.scannables.models.NotificareScannable

class ScannableActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notificare_scannable_activity)

        setTitle(R.string.notificare_scannable_title)
        supportActionBar?.show()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupNfcAdapter()
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                val tag = intent.data ?: run {
                    NotificareLogger.warning("Discovered a NFC tag but it did not contain a URL.")
                    return
                }

                handleScannableTag(tag.toString())
            }
        }
    }

    override fun onBackPressed() {
        NotificareScannables.notifyListeners(NotificareScannablesException.UserCancelledScannableSession())
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setupNfcAdapter() {
        val manager = getSystemService(Context.NFC_SERVICE) as? NfcManager
        nfcAdapter = manager?.defaultAdapter
    }

    private fun enableForegroundDispatch() {
        try {
            val intent = Intent(this, this::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            // val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        } catch (e: Exception) {
            NotificareLogger.error("Error enabling NFC foreground dispatch.", e)
        }
    }

    private fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (e: Exception) {
            NotificareLogger.error("Error disabling NFC foreground dispatch.", e)
        }
    }

    private fun handleScannableTag(tag: String) {
        NotificareScannables.fetchScannable(tag, object : NotificareCallback<NotificareScannable> {
            override fun onSuccess(result: NotificareScannable) {
                NotificareScannables.notifyListeners(result)
                finish()
            }

            override fun onFailure(e: Exception) {
                NotificareScannables.notifyListeners(e)
                finish()
            }
        })
    }
}
