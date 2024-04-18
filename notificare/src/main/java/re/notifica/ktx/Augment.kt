package re.notifica.ktx

import re.notifica.Notificare
import re.notifica.NotificareDeviceModule
import re.notifica.NotificareEventsModule
import re.notifica.internal.modules.NotificareCrashReporterModuleImpl
import re.notifica.internal.modules.NotificareDeviceModuleImpl
import re.notifica.internal.modules.NotificareEventsModuleImpl
import re.notifica.internal.modules.NotificareSessionModuleImpl

@Suppress("unused")
public fun Notificare.device(): NotificareDeviceModule {
    return NotificareDeviceModuleImpl
}

@Suppress("unused")
public fun Notificare.events(): NotificareEventsModule {
    return NotificareEventsModuleImpl
}

internal fun Notificare.deviceImplementation(): NotificareDeviceModuleImpl {
    return device() as NotificareDeviceModuleImpl
}

internal fun Notificare.eventsImplementation(): NotificareEventsModuleImpl {
    return events() as NotificareEventsModuleImpl
}

@Suppress("unused")
internal fun Notificare.session(): NotificareSessionModuleImpl {
    return NotificareSessionModuleImpl
}

@Suppress("unused")
internal fun Notificare.crashReporter(): NotificareCrashReporterModuleImpl {
    return NotificareCrashReporterModuleImpl
}
