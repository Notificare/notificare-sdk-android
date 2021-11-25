package re.notifica.scannables

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.internal.NotificareLogger
import re.notifica.internal.common.getEnum
import re.notifica.internal.common.getEnumExtra
import re.notifica.internal.common.putEnum
import re.notifica.scannables.ktx.scannables
import re.notifica.scannables.ktx.scannablesImplementation
import re.notifica.scannables.models.NotificareScannable

public class ScannableActivity : AppCompatActivity() {

    public companion object {
        internal const val EXTRA_MODE = "re.notifica.scannables.extra.ScanMode"
    }

    private var nfcAdapter: NfcAdapter? = null
    private var mode: ScanMode = ScanMode.QR_CODE
    private var handlingScannable = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mode = savedInstanceState?.getEnum<ScanMode>(EXTRA_MODE)
            ?: intent.getEnumExtra<ScanMode>(EXTRA_MODE)
                ?: ScanMode.QR_CODE

        when (mode) {
            ScanMode.NFC -> setContentView(R.layout.notificare_scannable_nfc_activity)
            ScanMode.QR_CODE -> setContentView(R.layout.notificare_scannable_qr_code_activity)
        }

        supportActionBar?.show()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        when (mode) {
            ScanMode.NFC -> setupNfcAdapter()
            ScanMode.QR_CODE -> {
                val manager = Notificare.scannablesImplementation().serviceManager ?: run {
                    val error = IllegalStateException("No scannables dependencies have been detected.")
                    Notificare.scannablesImplementation().notifyListeners(error)

                    finish()
                    return
                }

                supportFragmentManager.commit {
                    replace(R.id.fragment_container, manager.getQrCodeScannerFragmentClass(), null)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putEnum(EXTRA_MODE, mode)
    }

    override fun onResume() {
        super.onResume()

        if (mode == ScanMode.NFC) {
            enableForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()

        if (mode == ScanMode.NFC) {
            disableForegroundDispatch()
        }
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
        Notificare.scannablesImplementation()
            .notifyListeners(NotificareUserCancelledScannableSessionException())

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


    // region NFC

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

    // endregion


    public fun handleScannableTag(tag: String) {
        if (handlingScannable) return
        handlingScannable = true

        Notificare.scannables().fetch(tag, object : NotificareCallback<NotificareScannable> {
            override fun onSuccess(result: NotificareScannable) {
                Notificare.scannablesImplementation().notifyListeners(result)
                finish()
            }

            override fun onFailure(e: Exception) {
                Notificare.scannablesImplementation().notifyListeners(e)
                finish()
            }
        })
    }

    public fun handleScannableError(error: Exception) {
        Notificare.scannablesImplementation().notifyListeners(error)
    }


    internal enum class ScanMode {
        NFC,
        QR_CODE
    }
}
