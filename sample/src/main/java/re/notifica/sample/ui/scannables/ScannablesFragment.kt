package re.notifica.sample.ui.scannables

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import re.notifica.Notificare
import re.notifica.push.ui.ktx.pushUI
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentScannablesBinding
import re.notifica.scannables.NotificareScannables
import re.notifica.scannables.NotificareUserCancelledScannableSessionException
import re.notifica.scannables.ktx.scannables
import re.notifica.scannables.models.NotificareScannable
import timber.log.Timber

class ScannablesFragment : Fragment(), NotificareScannables.ScannableSessionListener {
    private lateinit var binding: FragmentScannablesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Notificare.scannables().addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        Notificare.scannables().removeListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScannablesBinding.inflate(inflater, container, false)

        checkNfcStatus()
        setupListeners()

        return binding.root
    }

    override fun onScannableDetected(scannable: NotificareScannable) {
        val notification = scannable.notification ?: run {
            Timber.i("Scannable without notification detected.")
            Snackbar.make(requireView(), "Scannable without notification detected.", Snackbar.LENGTH_SHORT)
                .show()

            return
        }

        Notificare.pushUI().presentNotification(requireActivity(), notification)
    }

    override fun onScannableSessionError(error: Exception) {
        if (error is NotificareUserCancelledScannableSessionException) {
            return
        }

        Timber.e(error, "Scannable session error.")
        Snackbar.make(requireView(), "Scannable session error: ${error.message}", Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun checkNfcStatus() {
        val nfcStatus = binding.nfcAvailableStatus

        if (Notificare.scannables().canStartNfcScannableSession) {
            nfcStatus.text = getString(R.string.scannables_nfc_and_qr_code_available)

            return
        }

        nfcStatus.text = getString(R.string.scannables_only_qr_code_available)
        binding.scannablesNfcButton.isEnabled = false
    }

    private fun setupListeners() {
        binding.scannablesNfcButton.setOnClickListener {
            Notificare.scannables().startNfcScannableSession(requireActivity())
        }

        binding.scannablesQrCodeButton.setOnClickListener {
            Notificare.scannables().startQrCodeScannableSession(requireActivity())
        }
    }
}
