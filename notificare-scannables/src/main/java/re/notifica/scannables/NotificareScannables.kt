package re.notifica.scannables

import android.app.Activity
import androidx.annotation.MainThread
import re.notifica.NotificareCallback
import re.notifica.scannables.models.NotificareScannable

public interface NotificareScannables {

    /**
     * Indicates whether an NFC scannable session can be started on the current device.
     *
     * Returns `true` if the device supports and is ready for starting an NFC scanning session, otherwise `false`.
     */
    public val canStartNfcScannableSession: Boolean

    /**
     * Adds a listener for scannable session events.
     *
     * The listener will receive events about the scanning process, such as when a scannable item
     * is detected or when an error occurs during the session. The listener should implement [ScannableSessionListener].
     *
     * @param listener The [ScannableSessionListener] to be added for session events.
     *
     * @see [ScannableSessionListener]
     */
    public fun addListener(listener: ScannableSessionListener)

    /**
     * Removes a previously added scannable session listener.
     *
     * Use this method to stop receiving scannable session events.
     *
     * @param listener The [ScannableSessionListener] to be removed.
     *
     * @see [ScannableSessionListener]
     */
    public fun removeListener(listener: ScannableSessionListener)

    /**
     * Starts a scannable session, automatically selecting the best scanning method available.
     *
     * If NFC is available, it starts an NFC-based scanning session. If NFC is not available, it defaults to starting
     * a QR code scanning session.
     *
     * @param activity The [Activity] context from which the scannable session will be launched.
     */
    public fun startScannableSession(activity: Activity)

    /**
     * Starts an NFC scannable session.
     *
     * Initiates an NFC-based scan, allowing the user to scan NFC tags. This will only function on
     * devices that support NFC and have it enabled.
     *
     * @param activity The [Activity] context from which the NFC scanning session will be launched.
     * @throws IllegalStateException If NFC is not available or enabled on the device.
     */
    public fun startNfcScannableSession(activity: Activity)

    /**
     * Starts a QR code scannable session.
     *
     * Initiates a QR code-based scan using the device camera, allowing the user to scan QR codes.
     *
     * @param activity The [Activity] context from which the QR code scanning session will be launched.
     */
    public fun startQrCodeScannableSession(activity: Activity)

    /**
     * Fetches a scannable item by its tag.
     *
     * @param tag The tag identifier for the scannable item to be fetched.
     * @return The [NotificareScannable] object corresponding to the provided tag.
     *
     * @see [NotificareScannable]
     */
    public suspend fun fetch(tag: String): NotificareScannable

    /**
     * Fetches a scannable item by its tag with a callback.
     *
     * @param tag The tag identifier for the scannable item to be fetched.
     * @param callback The [NotificareCallback] to be invoked with the result or error.
     *
     * @see [NotificareScannable]
     */
    public fun fetch(tag: String, callback: NotificareCallback<NotificareScannable>)

    /**
     * Interface for receiving notifications about scannable session events.
     *
     * Implement this listener to handle events related to the scanning session, such as when a scannable
     * item is detected (either via NFC or QR code), or when an error occurs during the session.
     */
    public interface ScannableSessionListener {

        /**
         * Called when a scannable item is detected during a scannable session.
         *
         * This method is triggered when either an NFC tag or a QR code is successfully scanned, and
         * the corresponding [NotificareScannable] is retrieved. This callback will be invoked on the main thread.
         *
         * @param scannable The detected [NotificareScannable] object.
         */
        @MainThread
        public fun onScannableDetected(scannable: NotificareScannable)

        /**
         * Called when an error occurs during a scannable session.
         *
         * This method is triggered if there's a failure while scanning or processing the scannable item,
         * either due to NFC or QR code scanning issues, or if the scannable item cannot be retrieved.
         * This callback will be invoked on the main thread.
         *
         * @param error The [Exception] that caused the error during the scannable session.
         */
        @MainThread
        public fun onScannableSessionError(error: Exception)
    }
}
