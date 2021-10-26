package re.notifica.geo

import android.location.Location
import re.notifica.InternalNotificareApi
import re.notifica.geo.internal.BeaconServiceManager
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion

public interface NotificareGeo {

    public val hasLocationServicesEnabled: Boolean

    public val hasBluetoothEnabled: Boolean

    public fun enableLocationUpdates()

    public fun disableLocationUpdates()

    public fun addListener(listener: Listener)

    public fun removeListener(listener: Listener)

    public interface Listener {
        public fun onLocationUpdated(location: NotificareLocation) {}

        public fun onEnterRegion(region: NotificareRegion) {}

        public fun onExitRegion(region: NotificareRegion) {}

        public fun onEnterBeacon(beacon: NotificareBeacon) {}

        public fun onExitBeacon(beacon: NotificareBeacon) {}

        public fun onBeaconsRanged(region: NotificareRegion, beacons: List<NotificareBeacon>) {}
    }
}

public interface NotificareInternalGeo {

    @InternalNotificareApi
    public companion object {
        public const val INTENT_ACTION_LOCATION_UPDATED: String = "re.notifica.intent.action.LocationUpdated"
        public const val INTENT_ACTION_GEOFENCE_TRANSITION: String = "re.notifica.intent.action.GeofenceTransition"

        public const val DEFAULT_LOCATION_UPDATES_INTERVAL: Long = (60 * 1000).toLong()
        public const val DEFAULT_LOCATION_UPDATES_FASTEST_INTERVAL: Long = (30 * 1000).toLong()
        public const val DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT: Double = 10.0
        public const val DEFAULT_GEOFENCE_RESPONSIVENESS: Int = 0
    }

    @InternalNotificareApi
    public fun handleLocationUpdate(location: Location)

    @InternalNotificareApi
    public fun handleRegionEnter(identifiers: List<String>)

    @InternalNotificareApi
    public fun handleRegionExit(identifiers: List<String>)

    @InternalNotificareApi
    public fun handleBeaconEnter(uniqueId: String, major: Int, minor: Int?)

    @InternalNotificareApi
    public fun handleBeaconExit(uniqueId: String, major: Int, minor: Int?)

    @InternalNotificareApi
    public fun handleRangingBeacons(regionId: String, beacons: List<BeaconServiceManager.Beacon>)
}
