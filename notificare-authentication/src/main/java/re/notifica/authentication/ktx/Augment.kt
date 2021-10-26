package re.notifica.authentication.ktx

import re.notifica.Notificare
import re.notifica.NotificareInternalEventsModule
import re.notifica.authentication.NotificareAuthentication
import re.notifica.authentication.internal.NotificareAuthenticationImpl
import re.notifica.ktx.events

@Suppress("unused")
public fun Notificare.authentication(): NotificareAuthentication {
    return NotificareAuthenticationImpl
}


internal fun Notificare.authenticationImplementation(): NotificareAuthenticationImpl {
    return authentication() as NotificareAuthenticationImpl
}

internal fun Notificare.eventsInternal(): NotificareInternalEventsModule {
    return events() as NotificareInternalEventsModule
}
