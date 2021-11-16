package re.notifica.scannables.fcm.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.util.isEmpty
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import re.notifica.internal.NotificareLogger
import re.notifica.scannables.ScannableActivity
import re.notifica.scannables.fcm.R

public class QrCodeScannerFragment : Fragment(R.layout.notificare_scannable_qr_code_fcm_fragment) {
    private lateinit var surfaceView: SurfaceView
    private var cameraSource: CameraSource? = null

    private val hasCameraPermission: Boolean
        get() {
            return ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            setupScanner()
        } else {
            val error = IllegalStateException("Barcode scanner is not operational.")
            (activity as? ScannableActivity)?.handleScannableError(error)
            activity?.finish()
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        surfaceView = view.findViewById(R.id.surface_view)

        // NOTE: Setting the surface view back to invisible will solve the issue of the missed surfaceCreated event
        // when dealing with the camera permissions.
        surfaceView.isInvisible = true

        if (hasCameraPermission) {
            setupScanner()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
    }


    private fun setupScanner() {
        val detector = BarcodeDetector.Builder(requireContext())
            .setBarcodeFormats(Barcode.PDF417 or Barcode.AZTEC or Barcode.CODE_128 or Barcode.QR_CODE)
            .build()

        if (!detector.isOperational) {
            val error = IllegalStateException("Barcode scanner is not operational.")
            (activity as? ScannableActivity)?.handleScannableError(error)
            activity?.finish()

            return
        }

        cameraSource = CameraSource.Builder(requireContext(), detector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setAutoFocusEnabled(true)
            .build()

        detector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                if (detections.detectedItems.isEmpty()) return

                val barcode = detections.detectedItems.valueAt(0)
                activity?.runOnUiThread {
                    (activity as? ScannableActivity)?.handleScannableTag(barcode.rawValue)
                }
            }
        })

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    cameraSource?.start(holder)
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to start camera source.", e)

                    cameraSource?.release()
                    cameraSource = null

                    (activity as? ScannableActivity)?.handleScannableError(e)
                    activity?.finish()
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource?.stop()
            }
        })

        // NOTE: Setting the surface view back to visible, after adding the holder callback, will cause it to render
        // and trigger the surfaceCreated event.
        surfaceView.isInvisible = false
    }
}
