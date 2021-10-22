package re.notifica.push.ktx

import re.notifica.Notificare
import re.notifica.NotificareInternalDeviceModule
import re.notifica.NotificareInternalEventsModule
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.push.NotificarePush
import re.notifica.push.internal.NotificarePushImpl

@Suppress("unused")
public fun Notificare.push(): NotificarePush {
    return NotificarePushImpl
}

internal fun Notificare.deviceInternal(): NotificareInternalDeviceModule {
    return device() as NotificareInternalDeviceModule
}

internal fun Notificare.eventsInternal(): NotificareInternalEventsModule {
    return events() as NotificareInternalEventsModule
}
