package re.notifica.scannables

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareLogger
import re.notifica.internal.common.putEnumExtra
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.modules.NotificareModule
import re.notifica.scannables.internal.network.push.FetchScannableResponse
import re.notifica.scannables.models.NotificareScannable

object NotificareScannables : NotificareModule() {
    internal var serviceManager: NotificareServiceManager? = null
        private set

    private val listeners = mutableListOf<ScannableSessionListener>()

    // region Notificare Module

    override fun configure() {
        serviceManager = NotificareServiceManager.Factory.create(Notificare.requireContext())
    }

    override suspend fun launch() {}

    override suspend fun unlaunch() {}

    // endregion

    val canStartNfcScannableSession: Boolean
        get() {
            if (!Notificare.isConfigured) {
                NotificareLogger.warning("You must configure Notificare before executing 'canStartNfcScannableSession'.")
                return false
            }

            val manager = Notificare.requireContext().getSystemService(Context.NFC_SERVICE) as? NfcManager
            val adapter = manager?.defaultAdapter ?: return false

            return adapter.isEnabled
        }

    fun addListener(listener: ScannableSessionListener) = listeners.add(listener)

    fun removeListener(listener: ScannableSessionListener) = listeners.remove(listener)


    fun startScannableSession(activity: Activity) {
        if (canStartNfcScannableSession) {
            startNfcScannableSession(activity)
        } else {
            startQrCodeScannableSession(activity)
        }
    }

    fun startNfcScannableSession(activity: Activity) {
        val intent = Intent(activity, ScannableActivity::class.java)
            .putEnumExtra(ScannableActivity.EXTRA_MODE, ScannableActivity.ScanMode.NFC)

        activity.startActivity(intent)
    }

    fun startQrCodeScannableSession(activity: Activity) {
        val intent = Intent(activity, ScannableActivity::class.java)
            .putEnumExtra(ScannableActivity.EXTRA_MODE, ScannableActivity.ScanMode.QR_CODE)

        activity.startActivity(intent)
    }

    suspend fun fetchScannable(tag: String): NotificareScannable = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/scannable/tag/${Uri.encode(tag)}")
            .query("deviceID", Notificare.deviceManager.currentDevice?.id)
            .query("userID", Notificare.deviceManager.currentDevice?.userId)
            .responseDecodable(FetchScannableResponse::class)
            .scannable
            .toModel()
    }

    fun fetchScannable(tag: String, callback: NotificareCallback<NotificareScannable>) {
        GlobalScope.launch {
            try {
                val scannable = fetchScannable(tag)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(scannable)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }


    internal fun notifyListeners(scannable: NotificareScannable) {
        listeners.forEach {
            it.onScannableDetected(scannable)
        }
    }

    internal fun notifyListeners(error: Exception) {
        listeners.forEach {
            it.onScannerSessionError(error)
        }
    }


    interface ScannableSessionListener {
        fun onScannableDetected(scannable: NotificareScannable)

        fun onScannerSessionError(error: Exception)
    }
}
