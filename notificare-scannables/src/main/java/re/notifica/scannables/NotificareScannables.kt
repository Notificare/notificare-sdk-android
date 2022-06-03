package re.notifica.scannables

import android.app.Activity
import androidx.annotation.MainThread
import re.notifica.NotificareCallback
import re.notifica.scannables.models.NotificareScannable

public interface NotificareScannables {
    public val canStartNfcScannableSession: Boolean

    public fun addListener(listener: ScannableSessionListener)

    public fun removeListener(listener: ScannableSessionListener)

    public fun startScannableSession(activity: Activity)

    public fun startNfcScannableSession(activity: Activity)

    public fun startQrCodeScannableSession(activity: Activity)

    public suspend fun fetch(tag: String): NotificareScannable

    public fun fetch(tag: String, callback: NotificareCallback<NotificareScannable>)

    public interface ScannableSessionListener {
        @MainThread
        public fun onScannableDetected(scannable: NotificareScannable)

        @MainThread
        public fun onScannableSessionError(error: Exception)
    }
}
