package re.notifica.scannables

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.scannables.internal.logger
import re.notifica.utilities.parcel.getEnum
import re.notifica.utilities.parcel.getEnumExtra
import re.notifica.utilities.parcel.putEnum
import re.notifica.scannables.ui.QrCodeScannerFragment
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

    private var onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Notificare.scannablesImplementation()
                .notifyListeners(NotificareUserCancelledScannableSessionException())

            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

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
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, QrCodeScannerFragment::class.java, null)
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
                    logger.warning("Discovered a NFC tag but it did not contain a URL.")
                    return
                }

                handleScannableTag(tag.toString())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
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
            val intent = Intent(this, this::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    Intent.FILL_IN_DATA or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    Intent.FILL_IN_DATA
                )
            }

            // val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))

            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        } catch (e: Exception) {
            logger.error("Error enabling NFC foreground dispatch.", e)
        }
    }

    private fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (e: Exception) {
            logger.error("Error disabling NFC foreground dispatch.", e)
        }
    }

    // endregion

    public fun handleScannableTag(tag: String) {
        if (handlingScannable) return
        handlingScannable = true

        Notificare.scannables().fetch(
            tag,
            object : NotificareCallback<NotificareScannable> {
                override fun onSuccess(result: NotificareScannable) {
                    Notificare.scannablesImplementation().notifyListeners(result)
                    finish()
                }

                override fun onFailure(e: Exception) {
                    Notificare.scannablesImplementation().notifyListeners(e)
                    finish()
                }
            }
        )
    }

    public fun handleScannableError(error: Exception) {
        Notificare.scannablesImplementation().notifyListeners(error)
    }

    internal enum class ScanMode {
        NFC,
        QR_CODE
    }
}
