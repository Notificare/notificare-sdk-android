package re.notifica

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.internal.NotificareLogger
import re.notifica.internal.ktx.parcelable
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDevice

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

    protected open fun onReady(context: Context, application: NotificareApplication) {
        NotificareLogger.info("Notificare is ready, please override onReady if you want to receive these intents.")
    }

    protected open fun onUnlaunched(context: Context) {
        NotificareLogger.info(
            "Notificare has finished un-launching, please override onUnlaunched if you want to receive these intents."
        )
    }

    protected open fun onDeviceRegistered(context: Context, device: NotificareDevice) {
        NotificareLogger.info(
            "Device registered to Notificare, please override onDeviceRegistered if you want to receive these intents."
        )
    }
}
