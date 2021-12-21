package re.notifica.geo.hms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.huawei.hms.location.Geofence
import com.huawei.hms.location.GeofenceData
import com.huawei.hms.location.LocationResult
import re.notifica.Notificare
import re.notifica.geo.NotificareInternalGeo
import re.notifica.geo.hms.ktx.geoInternal
import re.notifica.internal.NotificareLogger

internal class LocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificareInternalGeo.INTENT_ACTION_LOCATION_UPDATED -> {
                if (LocationResult.hasResult(intent)) {
                    val result = LocationResult.extractResult(intent)
                    val location = result.lastLocation

                    onLocationUpdated(location)
                }
            }
            NotificareInternalGeo.INTENT_ACTION_GEOFENCE_TRANSITION -> {
                val event = GeofenceData.getDataFromIntent(intent)
                if (event.isFailure) {
                    NotificareLogger.warning("Geofencing error: ${event.errorCode}")
                    return
                }

                when (event.conversion) {
                    Geofence.ENTER_GEOFENCE_CONVERSION -> onRegionEnter(event.convertingGeofenceList)
                    Geofence.EXIT_GEOFENCE_CONVERSION -> onRegionExit(event.convertingGeofenceList)
                }
            }
        }
    }

    private fun onLocationUpdated(location: Location) {
        NotificareLogger.debug("Location updated = (${location.latitude}, ${location.longitude})")
        Notificare.geoInternal().handleLocationUpdate(location)
    }

    private fun onRegionEnter(geofences: List<Geofence>) {
        NotificareLogger.debug("Received a region enter event for ${geofences.size} geofences.")
        Notificare.geoInternal().handleRegionEnter(geofences.map { it.uniqueId })
    }

    private fun onRegionExit(geofences: List<Geofence>) {
        NotificareLogger.debug("Received a region exit event for ${geofences.size} geofences.")
        Notificare.geoInternal().handleRegionExit(geofences.map { it.uniqueId })
    }
}
