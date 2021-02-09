package re.notifica.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.NotificareLogger
import re.notifica.models.NotificareDevice

open class NotificareIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificareDefinitions.Intent.Actions.READY -> onReady()
            NotificareDefinitions.Intent.Actions.DEVICE_REGISTERED -> {
                val device: NotificareDevice = requireNotNull(
                    intent.getParcelableExtra(NotificareDefinitions.Intent.Extras.DEVICE)
                )

                onDeviceRegistered(device)
            }
        }
    }

    protected open fun onReady() {
        NotificareLogger.info("Notificare is ready, please override onReady if you want to receive these intents.")
    }

    protected open fun onDeviceRegistered(device: NotificareDevice) {
        NotificareLogger.info("Device registered to Notificare, please override onDeviceRegistered if you want to receive these intents.")
    }
}
