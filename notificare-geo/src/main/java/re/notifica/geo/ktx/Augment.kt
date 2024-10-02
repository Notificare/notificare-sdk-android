package re.notifica.geo.ktx

import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.NotificareInternalEventsModule
import re.notifica.geo.NotificareGeo
import re.notifica.geo.NotificareInternalGeo
import re.notifica.geo.internal.NotificareGeoImpl
import re.notifica.ktx.events

@Suppress("unused")
public fun Notificare.geo(): NotificareGeo {
    return NotificareGeoImpl
}

internal fun Notificare.eventsInternal(): NotificareInternalEventsModule {
    return events() as NotificareInternalEventsModule
}

internal fun Notificare.geoInternal(): NotificareInternalGeo {
    return geo() as NotificareInternalGeo
}

// region Intent actions

@InternalNotificareApi
public val Notificare.INTENT_ACTION_INTERNAL_LOCATION_UPDATED: String
    get() = "re.notifica.intent.action.internal.LocationUpdated"

@InternalNotificareApi
public val Notificare.INTENT_ACTION_INTERNAL_GEOFENCE_TRANSITION: String
    get() = "re.notifica.intent.action.internal.GeofenceTransition"

public val Notificare.INTENT_ACTION_LOCATION_UPDATED: String
    get() = "re.notifica.intent.action.LocationUpdated"

public val Notificare.INTENT_ACTION_REGION_ENTERED: String
    get() = "re.notifica.intent.action.RegionEntered"

public val Notificare.INTENT_ACTION_REGION_EXITED: String
    get() = "re.notifica.intent.action.RegionExited"

public val Notificare.INTENT_ACTION_BEACON_ENTERED: String
    get() = "re.notifica.intent.action.BeaconEntered"

public val Notificare.INTENT_ACTION_BEACON_EXITED: String
    get() = "re.notifica.intent.action.BeaconExited"

public val Notificare.INTENT_ACTION_BEACONS_RANGED: String
    get() = "re.notifica.intent.action.BeaconsRanged"

public val Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED: String
    get() = "re.notifica.intent.action.BeaconNotificationOpened"

// endregion

// region Intent extras

public val Notificare.INTENT_EXTRA_LOCATION: String
    get() = "re.notifica.intent.extra.Location"

public val Notificare.INTENT_EXTRA_REGION: String
    get() = "re.notifica.intent.extra.Region"

public val Notificare.INTENT_EXTRA_BEACON: String
    get() = "re.notifica.intent.extra.Beacon"

public val Notificare.INTENT_EXTRA_RANGED_BEACONS: String
    get() = "re.notifica.intent.extra.RangedBeacons"

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
