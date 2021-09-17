package re.notifica.geo.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationResult
import re.notifica.geo.NotificareGeo
import re.notifica.internal.NotificareLogger

public class LocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificareGeo.INTENT_ACTION_LOCATION_UPDATED -> {
                if (LocationResult.hasResult(intent)) {
                    val result = LocationResult.extractResult(intent)
                    val location = result.lastLocation

                    onLocationUpdated(location)
                }
            }
            NotificareGeo.INTENT_ACTION_GEOFENCE_TRANSITION -> {
                val event = GeofencingEvent.fromIntent(intent)
                if (event.hasError()) {
                    NotificareLogger.warning("Geofencing error: ${event.errorCode}")
                    return
                }

                when (event.geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> onRegionEnter(event.triggeringGeofences)
                    Geofence.GEOFENCE_TRANSITION_EXIT -> onRegionExit(event.triggeringGeofences)
                }
            }
        }
    }

    private fun onLocationUpdated(location: Location) {
        NotificareLogger.debug("Location updated = (${location.latitude}, ${location.longitude})")
        NotificareGeo.handleLocationUpdate(location)
    }

    private fun onRegionEnter(geofences: List<Geofence>) {
        NotificareLogger.debug("Received a region enter event for ${geofences.size} geofences.")
        NotificareGeo.handleRegionEnter(geofences.map { it.requestId })
    }

    private fun onRegionExit(geofences: List<Geofence>) {
        NotificareLogger.debug("Received a region exit event for ${geofences.size} geofences.")
        NotificareGeo.handleRegionExit(geofences.map { it.requestId })
    }
}
