package re.notifica.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
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
import re.notifica.internal.NotificareLogger
import re.notifica.utilities.ktx.parcelable
import re.notifica.utilities.ktx.parcelableArrayList

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

    protected open fun onLocationUpdated(context: Context, location: NotificareLocation) {
        NotificareLogger.debug(
            "Location updated, please override onLocationUpdated if you want to receive these intents."
        )
    }

    protected open fun onRegionEntered(context: Context, region: NotificareRegion) {
        NotificareLogger.debug(
            "Entered a region, please override onRegionEntered if you want to receive these intents."
        )
    }

    protected open fun onRegionExited(context: Context, region: NotificareRegion) {
        NotificareLogger.debug("Exited a region, please override onRegionExited if you want to receive these intents.")
    }

    protected open fun onBeaconEntered(context: Context, beacon: NotificareBeacon) {
        NotificareLogger.debug(
            "Entered a beacon, please override onBeaconEntered if you want to receive these intents."
        )
    }

    protected open fun onBeaconExited(context: Context, beacon: NotificareBeacon) {
        NotificareLogger.debug("Exited a beacon, please override onBeaconExited if you want to receive these intents.")
    }

    protected open fun onBeaconsRanged(context: Context, region: NotificareRegion, beacons: List<NotificareBeacon>) {
        NotificareLogger.debug("Ranged beacons, please override onBeaconsRanged if you want to receive these intents.")
    }
}
