package re.notifica.geo.ktx

import re.notifica.Notificare
import re.notifica.NotificareInternalEventsModule
import re.notifica.geo.NotificareGeo
import re.notifica.geo.internal.NotificareGeoImpl
import re.notifica.internal.NotificareModule
import re.notifica.internal.modules.integrations.NotificareLoyaltyIntegration
import re.notifica.ktx.events

@Suppress("unused")
public fun Notificare.geo(): NotificareGeo {
    return NotificareGeoImpl
}


internal fun Notificare.eventsInternal(): NotificareInternalEventsModule {
    return events() as NotificareInternalEventsModule
}

internal fun Notificare.loyaltyIntegration(): NotificareLoyaltyIntegration? {
    return NotificareModule.Module.LOYALTY.instance as? NotificareLoyaltyIntegration
}
