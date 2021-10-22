package re.notifica.scannables

import android.app.Activity
import re.notifica.NotificareCallback
import re.notifica.scannables.models.NotificareScannable

public interface NotificareScannables {
    public val canStartNfcScannableSession: Boolean

    public fun addListener(listener: ScannableSessionListener)

    public fun removeListener(listener: ScannableSessionListener)

    public fun startScannableSession(activity: Activity)

    public fun startNfcScannableSession(activity: Activity)

    public fun startQrCodeScannableSession(activity: Activity)

    public suspend fun fetchScannable(tag: String): NotificareScannable

    public fun fetchScannable(tag: String, callback: NotificareCallback<NotificareScannable>)

    public interface ScannableSessionListener {
        public fun onScannableDetected(scannable: NotificareScannable)

        public fun onScannerSessionError(error: Exception)
    }
}
