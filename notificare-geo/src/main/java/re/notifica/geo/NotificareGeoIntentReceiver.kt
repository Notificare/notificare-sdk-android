package re.notifica.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.geo.internal.logger
import re.notifica.geo.ktx.INTENT_ACTION_BEACONS_RANGED
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_ENTERED
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_EXITED
import re.notifica.geo.ktx.INTENT_ACTION_LOCATION_UPDATED
import re.notifica.geo.ktx.INTENT_ACTION_REGION_ENTERED
import re.notifica.geo.ktx.INTENT_ACTION_REGION_EXITED
import re.notifica.geo.ktx.INTENT_EXTRA_BEACON
import re.notifica.geo.ktx.INTENT_EXTRA_LOCATION
import re.notifica.geo.ktx.INTENT_EXTRA_RANGED_BEACONS
import re.notifica.geo.ktx.INTENT_EXTRA_REGION
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion
import re.notifica.utilities.parcel.parcelable
import re.notifica.utilities.parcel.parcelableArrayList

/**
 * A broadcast receiver for handling location and proximity events from the Notificare SDK.
 *
 * Extend this class to receive location updates, region transitions, and beacon proximity events. Override
 * specific methods to handle each event as needed.
 */
public open class NotificareGeoIntentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Notificare.INTENT_ACTION_LOCATION_UPDATED -> {
                val location: NotificareLocation = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_LOCATION)
                )

                onLocationUpdated(context, location)
            }

            Notificare.INTENT_ACTION_REGION_ENTERED -> {
                val region: NotificareRegion = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_REGION)
                )

                onRegionEntered(context, region)
            }

            Notificare.INTENT_ACTION_REGION_EXITED -> {
                val region: NotificareRegion = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_REGION)
                )

                onRegionExited(context, region)
            }

            Notificare.INTENT_ACTION_BEACON_ENTERED -> {
                val beacon: NotificareBeacon = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_BEACON)
                )

                onBeaconEntered(context, beacon)
            }

            Notificare.INTENT_ACTION_BEACON_EXITED -> {
                val beacon: NotificareBeacon = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_BEACON)
                )

                onBeaconExited(context, beacon)
            }

            Notificare.INTENT_ACTION_BEACONS_RANGED -> {
                val region: NotificareRegion = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_REGION)
                )

                val beacons: List<NotificareBeacon> = requireNotNull(
                    intent.parcelableArrayList(Notificare.INTENT_EXTRA_RANGED_BEACONS)
                )

                onBeaconsRanged(context, region, beacons)
            }
        }
    }

    /**
     * Called when the device's location is updated.
     *
     * This method provides the latest [NotificareLocation] for the device. Override to handle location updates,
     * such as updating a map or notifying the user of location-based events.
     *
     * @param context The context in which the receiver is running.
     * @param location The updated [NotificareLocation] object representing the device's current location.
     */
    protected open fun onLocationUpdated(context: Context, location: NotificareLocation) {
        logger.debug(
            "Location updated, please override onLocationUpdated if you want to receive these intents."
        )
    }

    /**
     * Called when the device enters a monitored region.
     *
     * This method is triggered upon entering a predefined [NotificareRegion]. Override to handle entry events,
     * such as starting location-based services or updating the app state.
     *
     * @param context The context in which the receiver is running.
     * @param region The [NotificareRegion] the device has entered.
     */
    protected open fun onRegionEntered(context: Context, region: NotificareRegion) {
        logger.debug(
            "Entered a region, please override onRegionEntered if you want to receive these intents."
        )
    }

    /**
     * Called when the device exits a monitored region.
     *
     * This method is triggered upon leaving a predefined [NotificareRegion]. Override to handle exit events,
     * such as stopping location-based services or logging transitions.
     *
     * @param context The context in which the receiver is running.
     * @param region The [NotificareRegion] the device has exited.
     */
    protected open fun onRegionExited(context: Context, region: NotificareRegion) {
        logger.debug("Exited a region, please override onRegionExited if you want to receive these intents.")
    }

    /**
     * Called when the device detects a nearby beacon and enters its proximity.
     *
     * This method is triggered when the device enters the range of a [NotificareBeacon]. Override to handle
     * beacon proximity entry events, such as triggering beacon-based notifications or app actions.
     *
     * @param context The context in which the receiver is running.
     * @param beacon The [NotificareBeacon] that the device has entered.
     */
    protected open fun onBeaconEntered(context: Context, beacon: NotificareBeacon) {
        logger.debug(
            "Entered a beacon, please override onBeaconEntered if you want to receive these intents."
        )
    }

    /**
     * Called when the device exits the proximity range of a beacon.
     *
     * This method is triggered when the device leaves the range of a [NotificareBeacon]. Override to handle
     * beacon proximity exit events, such as stopping beacon-based interactions or logging exit data.
     *
     * @param context The context in which the receiver is running.
     * @param beacon The [NotificareBeacon] that the device has exited.
     */
    protected open fun onBeaconExited(context: Context, beacon: NotificareBeacon) {
        logger.debug("Exited a beacon, please override onBeaconExited if you want to receive these intents.")
    }

    /**
     * Called when a range of beacons is detected within a specified region.
     *
     * This method provides a list of [NotificareBeacon] instances within a [NotificareRegion]. Override to handle
     * ranged beacons, such as updating a list of nearby beacons or performing proximity-based actions.
     *
     * @param context The context in which the receiver is running.
     * @param region The [NotificareRegion] within which the beacons were ranged.
     * @param beacons The list of [NotificareBeacon] instances currently in range.
     */
    protected open fun onBeaconsRanged(context: Context, region: NotificareRegion, beacons: List<NotificareBeacon>) {
        logger.debug("Ranged beacons, please override onBeaconsRanged if you want to receive these intents.")
    }
}
