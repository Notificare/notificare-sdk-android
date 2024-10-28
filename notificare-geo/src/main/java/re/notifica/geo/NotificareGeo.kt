package re.notifica.geo

import android.location.Location
import androidx.annotation.MainThread
import re.notifica.InternalNotificareApi
import re.notifica.geo.internal.BeaconServiceManager
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion

public interface NotificareGeo {

    /**
     * Specifies the intent receiver class for handling geolocation intents.
     *
     * This property defines the class that will receive and process the intents related to geolocation services.
     * The class must extend [NotificareGeoIntentReceiver].
     */
    public var intentReceiver: Class<out NotificareGeoIntentReceiver>

    /**
     * Indicates whether location services are enabled.
     *
     * This property returns `true` if the location services are enabled and accessible by the application, and `false`
     * otherwise.
     */
    public val hasLocationServicesEnabled: Boolean

    /**
     * Indicates whether Bluetooth is enabled.
     *
     * This property returns `true` if Bluetooth is enabled and available for beacon detection and ranging, and `false`
     * otherwise.
     */
    public val hasBluetoothEnabled: Boolean

    /**
     * Provides a list of regions currently being monitored.
     *
     * This property returns a list of [NotificareRegion] objects representing the geographical regions being actively
     * monitored for entry and exit events.
     *
     * @see [NotificareRegion]
     */
    public val monitoredRegions: List<NotificareRegion>

    /**
     * Provides a list of regions the user has entered.
     *
     * This property returns a list of [NotificareRegion] objects representing the regions that the user has entered and
     * not yet exited.
     *
     * @see [NotificareRegion]
     */
    public val enteredRegions: List<NotificareRegion>

    /**
     * Enables location updates.
     *
     * This method activates the system to start receiving location updates, monitoring regions, and detecting nearby
     * beacons.
     * Starting with Android 12 (API level 31), this function requires the developer to explicitly request
     * location access permission from the user. This request should be made before calling this method.
     *
     */
    public fun enableLocationUpdates()

    /**
     * Disables location updates.
     *
     * This method stops receiving location updates, monitoring regions, and detecting nearby beacons.
     */
    public fun disableLocationUpdates()

    /**
     * Adds a geolocation listener.
     *
     * This method registers a [Listener] to receive callbacks related to location updates, region monitoring events,
     * and beacon proximity events.
     *
     * @param listener The [Listener] to add for receiving geolocation events.
     *
     * @see [Listener]
     */
    public fun addListener(listener: Listener)

    /**
     * Removes a geolocation listener.
     *
     * This method unregisters a previously added [Listener] to stop receiving callbacks related to location updates,
     * region monitoring events, and beacon proximity events.
     *
     * @param listener The [Listener] to remove.
     *
     * @see [Listener]
     */
    public fun removeListener(listener: Listener)

    /**
     * Listener interface for receiving geolocation and beacon events.
     *
     * Implement this interface to handle location updates, region monitoring events, and beacon ranging events.
     */
    public interface Listener {
        /**
         * Called when a new location update is received.
         *
         * @param location The updated [NotificareLocation] representing the user's new location.
         *
         * @see [NotificareLocation]
         */
        @MainThread
        public fun onLocationUpdated(location: NotificareLocation) {
        }

        /**
         * Called when the user enters a monitored region.
         *
         * @param region The [NotificareRegion] representing the region the user has entered.
         *
         * @see [NotificareRegion]
         */
        @MainThread
        public fun onRegionEntered(region: NotificareRegion) {
        }

        /**
         * Called when the user exits a monitored region.
         *
         * @param region The [NotificareRegion] representing the region the user has exited.
         *
         * @see [NotificareRegion]
         */
        @MainThread
        public fun onRegionExited(region: NotificareRegion) {
        }

        /**
         * Called when the user enters the proximity of a beacon.
         *
         * @param beacon The [NotificareBeacon] representing the beacon the user has entered the proximity of.
         *
         * @see [NotificareBeacon]
         */
        @MainThread
        public fun onBeaconEntered(beacon: NotificareBeacon) {
        }

        /**
         * Called when the user exits the proximity of a beacon.
         *
         * @param beacon The [NotificareBeacon] representing the beacon the user has exited the proximity of.
         *
         * @see [NotificareBeacon]
         */
        @MainThread
        public fun onBeaconExited(beacon: NotificareBeacon) {
        }

        /**
         * Called when beacons are ranged in a monitored region.
         *
         * This method provides the list of beacons currently detected within the given region.
         *
         * @param region The [NotificareRegion] where beacons were ranged.
         * @param beacons A list of [NotificareBeacon] that were detected in the region.
         *
         * @see [NotificareRegion]
         * @see [NotificareBeacon]
         */
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
