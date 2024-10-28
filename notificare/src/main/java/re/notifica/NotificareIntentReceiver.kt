package re.notifica

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.internal.logger
import re.notifica.utilities.parcel.parcelable
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDevice

/**
 * A broadcast receiver for handling Notificare-specific intents related to the SDK lifecycle and device events.
 *
 * This class provides entry points for receiving events such as SDK readiness, unlaunching, and device registration.
 * Extend this class and override the provided methods to customize behavior for each event.
 */
public open class NotificareIntentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Notificare.INTENT_ACTION_READY -> {
                val application: NotificareApplication = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_APPLICATION)
                )

                onReady(context, application)
            }
            Notificare.INTENT_ACTION_UNLAUNCHED -> onUnlaunched(context)
            Notificare.INTENT_ACTION_DEVICE_REGISTERED -> {
                val device: NotificareDevice = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_DEVICE)
                )

                onDeviceRegistered(context, device)
            }
        }
    }

    /**
     * Called when the Notificare SDK is launched and fully ready.
     *
     * This method is triggered when the SDK has completed initialization and the [NotificareApplication]
     * instance is available. Override to perform actions when the SDK is ready.
     *
     * @param context The context in which the receiver is running.
     * @param application The [NotificareApplication] instance containing the application's metadata.
     */
    protected open fun onReady(context: Context, application: NotificareApplication) {
        logger.info("Notificare is ready, please override onReady if you want to receive these intents.")
    }

    /**
     * Called when the Notificare SDK has been unlaunched.
     *
     * This method is triggered when the SDK has been shut down, indicating that it is no longer active.
     * Override this method to perform cleanup or update the app state based on the SDK's unlaunching.
     *
     * @param context The context in which the receiver is running.
     */
    protected open fun onUnlaunched(context: Context) {
        logger.info(
            "Notificare has finished un-launching, please override onUnlaunched if you want to receive these intents."
        )
    }

    /**
     * Called when the device has been successfully registered with the Notificare platform.
     *
     * This method is triggered after the device has been registered, making it eligible to receive notifications
     * and participate in device-specific interactions. Override this method to handle registration events, such as
     * storing device data or updating the UI.
     *
     * @param context The context in which the receiver is running.
     * @param device The registered [NotificareDevice] instance representing the device's registration details.
     */
    protected open fun onDeviceRegistered(context: Context, device: NotificareDevice) {
        logger.info(
            "Device registered to Notificare, please override onDeviceRegistered if you want to receive these intents."
        )
    }
}
