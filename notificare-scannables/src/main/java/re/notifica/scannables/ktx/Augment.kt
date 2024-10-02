package re.notifica.scannables.ktx

import re.notifica.Notificare
import re.notifica.scannables.NotificareScannables
import re.notifica.scannables.internal.NotificareScannablesImpl

@Suppress("unused")
public fun Notificare.scannables(): NotificareScannables {
    return NotificareScannablesImpl
}

internal fun Notificare.scannablesImplementation(): NotificareScannablesImpl {
    return scannables() as NotificareScannablesImpl
}
