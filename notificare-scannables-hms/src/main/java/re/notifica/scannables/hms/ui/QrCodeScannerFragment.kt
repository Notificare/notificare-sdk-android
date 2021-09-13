package re.notifica.scannables.hms.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.huawei.hms.hmsscankit.RemoteView
import com.huawei.hms.ml.scan.HmsScan
import re.notifica.scannables.ScannableActivity
import re.notifica.scannables.hms.R

public class QrCodeScannerFragment : Fragment(R.layout.notificare_scannable_qr_code_hms_fragment) {
    private lateinit var cameraView: ViewGroup
    private lateinit var remoteView: RemoteView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraView = view.findViewById(R.id.camera_view)

        setupScanner(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        remoteView.onStart()
    }

    override fun onResume() {
        super.onResume()
        remoteView.onResume()
    }

    override fun onPause() {
        super.onPause()
        remoteView.onPause()
    }

    override fun onStop() {
        super.onStop()
        remoteView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteView.onDestroy()
    }


    private fun setupScanner(savedInstanceState: Bundle?) {
        remoteView = RemoteView.Builder()
            .setContext(requireActivity())
            .setFormat(
                HmsScan.PDF417_SCAN_TYPE,
                HmsScan.AZTEC_SCAN_TYPE,
                HmsScan.CODE128_SCAN_TYPE,
                HmsScan.QRCODE_SCAN_TYPE
            )
            .build()

        remoteView.onCreate(savedInstanceState)

        remoteView.setOnResultCallback { results ->
            if (results.isEmpty()) return@setOnResultCallback

            val barcode = results[0]
            remoteView.pauseContinuouslyScan()

            activity?.runOnUiThread {
                (activity as? ScannableActivity)?.handleScannableTag(barcode.originalValue)
            }
        }

        cameraView.addView(remoteView)
    }
}
