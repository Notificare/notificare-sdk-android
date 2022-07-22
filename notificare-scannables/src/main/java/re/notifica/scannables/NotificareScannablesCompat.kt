package re.notifica.scannables

import android.app.Activity
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.scannables.ktx.scannables
import re.notifica.scannables.models.NotificareScannable

public object NotificareScannablesCompat {

    @JvmStatic
    public val canStartNfcScannableSession: Boolean
        get() = Notificare.scannables().canStartNfcScannableSession

    @JvmStatic
    public fun addListener(listener: NotificareScannables.ScannableSessionListener) {
        Notificare.scannables().addListener(listener)
    }

    @JvmStatic
    public fun removeListener(listener: NotificareScannables.ScannableSessionListener) {
        Notificare.scannables().removeListener(listener)
    }

    @JvmStatic
    public fun startScannableSession(activity: Activity) {
        Notificare.scannables().startScannableSession(activity)
    }

    @JvmStatic
    public fun startNfcScannableSession(activity: Activity) {
        Notificare.scannables().startNfcScannableSession(activity)
    }

    @JvmStatic
    public fun startQrCodeScannableSession(activity: Activity) {
        Notificare.scannables().startQrCodeScannableSession(activity)
    }

    @JvmStatic
    public fun fetch(tag: String, callback: NotificareCallback<NotificareScannable>) {
        Notificare.scannables().fetch(tag, callback)
    }
}
