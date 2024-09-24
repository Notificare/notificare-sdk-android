package re.notifica.internal

import re.notifica.Notificare
import re.notifica.NotificareConfigurationProvider
import re.notifica.internal.modules.NotificareCrashReporterModuleImpl
import re.notifica.internal.modules.NotificareDeviceModuleImpl
import re.notifica.internal.modules.NotificareEventsModuleImpl
import re.notifica.internal.modules.NotificareSessionModuleImpl
import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "Notificare",
).apply {
    labelClassIgnoreList = listOf(
        Notificare::class,
        NotificareConfigurationProvider::class,
        NotificareCrashReporterModuleImpl::class,
        NotificareDeviceModuleImpl::class,
        NotificareEventsModuleImpl::class,
        NotificareSessionModuleImpl::class,
    )
}
