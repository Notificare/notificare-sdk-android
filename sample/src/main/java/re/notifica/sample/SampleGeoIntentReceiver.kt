package re.notifica.sample

import android.content.Context
import android.util.Log
import re.notifica.geo.NotificareGeoIntentReceiver
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion

class SampleGeoIntentReceiver : NotificareGeoIntentReceiver() {
    companion object {
        private val TAG = SampleGeoIntentReceiver::class.simpleName
    }

    override fun onLocationUpdated(context: Context, location: NotificareLocation) {
        Log.w(TAG, "location updated = $location")
    }

    override fun onRegionEntered(context: Context, region: NotificareRegion) {
        Log.w(TAG, "region entered = $region")
    }

    override fun onRegionExited(context: Context, region: NotificareRegion) {
        Log.w(TAG, "region exited = $region")
    }

    override fun onBeaconEntered(context: Context, beacon: NotificareBeacon) {
        Log.w(TAG, "beacon entered = $beacon")
    }

    override fun onBeaconExited(context: Context, beacon: NotificareBeacon) {
        Log.w(TAG, "beacon exited = $beacon")
    }

    override fun onBeaconsRanged(context: Context, region: NotificareRegion, beacons: List<NotificareBeacon>) {
        Log.w(TAG, "beacons ranged")
        Log.w(TAG, "region = $region")
        Log.w(TAG, "beacons = $beacons")
    }
}
