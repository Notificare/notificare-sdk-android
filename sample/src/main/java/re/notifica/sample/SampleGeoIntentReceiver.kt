package re.notifica.sample

import android.content.Context
import re.notifica.geo.NotificareGeoIntentReceiver
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion
import timber.log.Timber

class SampleGeoIntentReceiver : NotificareGeoIntentReceiver() {

    override fun onLocationUpdated(context: Context, location: NotificareLocation) {
        Timber.d("location updated = $location")
    }

    override fun onRegionEntered(context: Context, region: NotificareRegion) {
        Timber.d("region entered = $region")
    }

    override fun onRegionExited(context: Context, region: NotificareRegion) {
        Timber.d("region exited = $region")
    }

    override fun onBeaconEntered(context: Context, beacon: NotificareBeacon) {
        Timber.d("beacon entered = $beacon")
    }

    override fun onBeaconExited(context: Context, beacon: NotificareBeacon) {
        Timber.d("beacon exited = $beacon")
    }

    override fun onBeaconsRanged(context: Context, region: NotificareRegion, beacons: List<NotificareBeacon>) {
        Timber.d("beacons ranged")
        Timber.d("region = $region")
        Timber.d("beacons = $beacons")
    }
}
