package re.notifica.geo

import android.location.Location
import androidx.annotation.MainThread
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
        @MainThread
        public fun onLocationUpdated(location: NotificareLocation) {
        }

        @MainThread
        public fun onRegionEntered(region: NotificareRegion) {
        }

        @MainThread
        public fun onRegionExited(region: NotificareRegion) {
        }

        @MainThread
        public fun onBeaconEntered(beacon: NotificareBeacon) {
        }

        @MainThread
        public fun onBeaconExited(beacon: NotificareBeacon) {
        }

        @MainThread
        public fun onBeaconsRanged(region: NotificareRegion, beacons: List<NotificareBeacon>) {
        }
    }
}

public interface NotificareInternalGeo {
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
