package re.notifica.scannables.internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcManager
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.common.onMainThread
import re.notifica.internal.common.putEnumExtra
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.scannables.NotificareScannables
import re.notifica.scannables.ScannableActivity
import re.notifica.scannables.internal.network.push.FetchScannableResponse
import re.notifica.scannables.models.NotificareScannable
import java.lang.ref.WeakReference

@Keep
internal object NotificareScannablesImpl : NotificareModule(), NotificareScannables {
    internal var serviceManager: ServiceManager? = null
        private set

    private val listeners = mutableListOf<WeakReference<NotificareScannables.ScannableSessionListener>>()

    // region Notificare Module

    override fun configure() {
        serviceManager = ServiceManager.create()
    }

    // endregion

    // region Notificare Scannables Module

    override val canStartNfcScannableSession: Boolean
        get() {
            if (!Notificare.isConfigured) {
                NotificareLogger.warning(
                    "You must configure Notificare before executing 'canStartNfcScannableSession'."
                )
                return false
            }

            val manager = Notificare.requireContext().getSystemService(Context.NFC_SERVICE) as? NfcManager
            val adapter = manager?.defaultAdapter ?: return false

            return adapter.isEnabled
        }

    override fun addListener(listener: NotificareScannables.ScannableSessionListener) {
        listeners.add(WeakReference(listener))
    }

    override fun removeListener(listener: NotificareScannables.ScannableSessionListener) {
        listeners.forEach { reference ->
            if (reference.get() == null || reference.get() == listener)
                listeners.remove(reference)
        }
    }

    override fun startScannableSession(activity: Activity) {
        if (canStartNfcScannableSession) {
            startNfcScannableSession(activity)
        } else {
            startQrCodeScannableSession(activity)
        }
    }

    override fun startNfcScannableSession(activity: Activity) {
        val intent = Intent(activity, ScannableActivity::class.java)
            .putEnumExtra(ScannableActivity.EXTRA_MODE, ScannableActivity.ScanMode.NFC)

        activity.startActivity(intent)
    }

    override fun startQrCodeScannableSession(activity: Activity) {
        val intent = Intent(activity, ScannableActivity::class.java)
            .putEnumExtra(ScannableActivity.EXTRA_MODE, ScannableActivity.ScanMode.QR_CODE)

        activity.startActivity(intent)
    }

    override suspend fun fetch(tag: String): NotificareScannable = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/scannable/tag/${Uri.encode(tag)}")
            .query("deviceID", Notificare.device().currentDevice?.id)
            .query("userID", Notificare.device().currentDevice?.userId)
            .responseDecodable(FetchScannableResponse::class)
            .scannable
            .toModel()
    }

    override fun fetch(tag: String, callback: NotificareCallback<NotificareScannable>): Unit =
        toCallbackFunction(::fetch)(tag, callback)

    // endregion

    internal fun notifyListeners(scannable: NotificareScannable) {
        onMainThread {
            listeners.forEach {
                it.get()?.onScannableDetected(scannable)
            }
        }
    }

    internal fun notifyListeners(error: Exception) {
        onMainThread {
            listeners.forEach {
                it.get()?.onScannableSessionError(error)
            }
        }
    }
}
