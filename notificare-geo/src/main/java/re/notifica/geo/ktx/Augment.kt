package re.notifica.geo.ktx

import re.notifica.InternalNotificareApi
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

@Suppress("unused")
internal fun Notificare.loyaltyIntegration(): NotificareLoyaltyIntegration? {
    return NotificareModule.Module.LOYALTY.instance as? NotificareLoyaltyIntegration
}

// region Intent actions

@InternalNotificareApi
public val Notificare.INTENT_ACTION_LOCATION_UPDATED: String
    get() = "re.notifica.intent.action.LocationUpdated"

@InternalNotificareApi
public val Notificare.INTENT_ACTION_GEOFENCE_TRANSITION: String
    get() = "re.notifica.intent.action.GeofenceTransition"

// endregion

// region Default values

@InternalNotificareApi
public val Notificare.DEFAULT_LOCATION_UPDATES_INTERVAL: Long
    get() = (60 * 1000).toLong()

@InternalNotificareApi
public val Notificare.DEFAULT_LOCATION_UPDATES_FASTEST_INTERVAL: Long
    get() = (30 * 1000).toLong()

@InternalNotificareApi
public val Notificare.DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT: Double
    get() = 10.0

@InternalNotificareApi
public val Notificare.DEFAULT_GEOFENCE_RESPONSIVENESS: Int
    get() = 0

// endregion
