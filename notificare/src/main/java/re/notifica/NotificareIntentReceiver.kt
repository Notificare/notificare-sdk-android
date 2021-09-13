package re.notifica

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDevice

public open class NotificareIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            INTENT_ACTION_READY -> {
                val application: NotificareApplication = requireNotNull(
                    intent.getParcelableExtra(INTENT_EXTRA_APPLICATION)
                )

                onReady(application)
            }
            INTENT_ACTION_DEVICE_REGISTERED -> {
                val device: NotificareDevice = requireNotNull(
                    intent.getParcelableExtra(INTENT_EXTRA_DEVICE)
                )

                onDeviceRegistered(device)
            }
        }
    }

    protected open fun onReady(application: NotificareApplication) {
        NotificareLogger.info("Notificare is ready, please override onReady if you want to receive these intents.")
    }

    protected open fun onDeviceRegistered(device: NotificareDevice) {
        NotificareLogger.info("Device registered to Notificare, please override onDeviceRegistered if you want to receive these intents.")
    }

    public companion object {
        internal const val INTENT_ACTION_READY = "re.notifica.intent.action.Ready"
        internal const val INTENT_ACTION_DEVICE_REGISTERED = "re.notifica.intent.action.DeviceRegistered"

        internal const val INTENT_EXTRA_APPLICATION = "re.notifica.intent.extra.Application"
        internal const val INTENT_EXTRA_DEVICE = "re.notifica.intent.extra.Device"
    }
}
