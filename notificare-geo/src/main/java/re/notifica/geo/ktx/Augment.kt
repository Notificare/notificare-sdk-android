package re.notifica.geo.ktx

import re.notifica.Notificare
import re.notifica.NotificareInternalEventsModule
import re.notifica.geo.NotificareGeo
import re.notifica.geo.internal.NotificareGeoImpl
import re.notifica.ktx.events

@Suppress("unused")
public fun Notificare.geo(): NotificareGeo {
    return NotificareGeoImpl
}


internal fun Notificare.eventsInternal(): NotificareInternalEventsModule {
    return events() as NotificareInternalEventsModule
}
