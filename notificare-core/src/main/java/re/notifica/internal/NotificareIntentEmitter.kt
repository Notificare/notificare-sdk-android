package re.notifica.internal

import android.content.Intent
import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.models.NotificareDevice

internal object NotificareIntentEmitter {
    fun onReady() {
        Notificare.requireContext().sendBroadcast(
            Intent(Notificare.requireContext(), Notificare.intentReceiver)
                .setAction(NotificareDefinitions.Intent.Actions.READY)
        )
    }

    fun onDeviceRegistered(device: NotificareDevice) {
        Notificare.requireContext().sendBroadcast(
            Intent(Notificare.requireContext(), Notificare.intentReceiver)
                .setAction(NotificareDefinitions.Intent.Actions.DEVICE_REGISTERED)
                .putExtra(NotificareDefinitions.Intent.Extras.DEVICE, device)
        )
    }
}
