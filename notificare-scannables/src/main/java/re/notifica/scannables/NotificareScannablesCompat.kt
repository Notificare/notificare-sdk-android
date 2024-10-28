package re.notifica.scannables

import android.app.Activity
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.scannables.ktx.scannables
import re.notifica.scannables.models.NotificareScannable

public object NotificareScannablesCompat {

    /**
     * Indicates whether an NFC scannable session can be started on the current device.
     *
     * Returns `true` if the device supports and is ready for starting an NFC scanning session, otherwise `false`.
     */
    @JvmStatic
    public val canStartNfcScannableSession: Boolean
        get() = Notificare.scannables().canStartNfcScannableSession

    /**
     * Adds a listener for scannable session events.
     *
     * The listener will receive notifications about the scanning process, such as when a scannable item
     * is detected or when an error occurs during the session. The listener should implement
     * [NotificareScannables.ScannableSessionListener].
     *
     * @param listener The [NotificareScannables.ScannableSessionListener] to be added for session event notifications.
     *
     * @see [NotificareScannables.ScannableSessionListener]
     */
    @JvmStatic
    public fun addListener(listener: NotificareScannables.ScannableSessionListener) {
        Notificare.scannables().addListener(listener)
    }

    /**
     * Removes a previously added scannable session listener.
     *
     * Use this method to stop receiving scannable session event notifications.
     *
     * @param listener The [NotificareScannables.ScannableSessionListener] to be removed.
     *
     * @see [NotificareScannables.ScannableSessionListener]
     */
    @JvmStatic
    public fun removeListener(listener: NotificareScannables.ScannableSessionListener) {
        Notificare.scannables().removeListener(listener)
    }

    /**
     * Starts a scannable session, automatically selecting the best scanning method available.
     *
     * If NFC is available, it starts an NFC-based scanning session. If NFC is not available, it defaults to starting
     * a QR code scanning session.
     *
     * @param activity The [Activity] context from which the scannable session will be launched.
     */
    @JvmStatic
    public fun startScannableSession(activity: Activity) {
        Notificare.scannables().startScannableSession(activity)
    }

    /**
     * Starts an NFC scannable session.
     *
     * Initiates an NFC-based scan, allowing the user to scan NFC tags. This will only function on
     * devices that support NFC and have it enabled.
     *
     * @param activity The [Activity] context from which the NFC scanning session will be launched.
     * @throws IllegalStateException If NFC is not available or enabled on the device.
     */
    @JvmStatic
    public fun startNfcScannableSession(activity: Activity) {
        Notificare.scannables().startNfcScannableSession(activity)
    }

    /**
     * Starts a QR code scannable session.
     *
     * Initiates a QR code-based scan using the device camera, allowing the user to scan QR codes.
     *
     * @param activity The [Activity] context from which the QR code scanning session will be launched.
     */
    @JvmStatic
    public fun startQrCodeScannableSession(activity: Activity) {
        Notificare.scannables().startQrCodeScannableSession(activity)
    }

    /**
     * Fetches a scannable item by its tag with a callback.
     *
     * @param tag The tag identifier for the scannable item to be fetched.
     * @param callback The [NotificareCallback] to be invoked with the result or error.
     *
     * @see [NotificareScannable]
     */
    @JvmStatic
    public fun fetch(tag: String, callback: NotificareCallback<NotificareScannable>) {
        Notificare.scannables().fetch(tag, callback)
    }
}
