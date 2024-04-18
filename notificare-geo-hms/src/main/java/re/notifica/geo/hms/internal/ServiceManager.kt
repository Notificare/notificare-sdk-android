package re.notifica.geo.hms.internal

import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.annotation.Keep
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.Geofence
import com.huawei.hms.location.GeofenceRequest
import com.huawei.hms.location.GeofenceService
import com.huawei.hms.location.LocationRequest
import com.huawei.hms.location.LocationServices
import kotlinx.coroutines.Deferred
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.geo.hms.LocationReceiver
import re.notifica.geo.hms.internal.ktx.asDeferred
import re.notifica.geo.hms.ktx.geoInternal
import re.notifica.geo.internal.ServiceManager
import re.notifica.geo.ktx.DEFAULT_GEOFENCE_RESPONSIVENESS
import re.notifica.geo.ktx.DEFAULT_LOCATION_UPDATES_FASTEST_INTERVAL
import re.notifica.geo.ktx.DEFAULT_LOCATION_UPDATES_INTERVAL
import re.notifica.geo.ktx.DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT
import re.notifica.geo.ktx.INTENT_ACTION_GEOFENCE_TRANSITION
import re.notifica.geo.ktx.INTENT_ACTION_INTERNAL_LOCATION_UPDATED
import re.notifica.geo.models.NotificareRegion
import re.notifica.internal.NotificareLogger

@Keep
@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    private val fusedLocationClient: FusedLocationProviderClient
    private val geofencingClient: GeofenceService

    private var locationUpdatesStarted = false
    private val locationPendingIntent: PendingIntent
    private val geofencingPendingIntent: PendingIntent

    override val available: Boolean
        get() = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    init {
        val context = Notificare.requireContext()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        geofencingClient = LocationServices.getGeofenceService(context)

        // region Setup location pending intent

        val locationIntent = Intent(context, LocationReceiver::class.java)
            .setAction(Notificare.INTENT_ACTION_INTERNAL_LOCATION_UPDATED)

        locationPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                locationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                0,
                locationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // endregion

        // region Setup geofencing pending intent

        val geofencingIntent = Intent(context, LocationReceiver::class.java)
            .setAction(Notificare.INTENT_ACTION_GEOFENCE_TRANSITION)

        geofencingPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                geofencingIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                0,
                geofencingIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // endregion
    }

    override fun enableLocationUpdates() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Notificare.geoInternal().handleLocationUpdate(location)
            } else {
                NotificareLogger.warning("No location found yet.")
            }

            if (locationUpdatesStarted) {
                NotificareLogger.debug("Location updates were previously enabled. Skipping...")
                return@addOnSuccessListener
            }

            val request = LocationRequest.create()
                .setInterval(Notificare.DEFAULT_LOCATION_UPDATES_INTERVAL)
                .setFastestInterval(Notificare.DEFAULT_LOCATION_UPDATES_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setSmallestDisplacement(Notificare.DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT.toFloat())

            fusedLocationClient.requestLocationUpdates(request, locationPendingIntent)
                .addOnSuccessListener {
                    NotificareLogger.info("Location updates started.")
                    locationUpdatesStarted = true
                }
                .addOnFailureListener {
                    NotificareLogger.error("Location updates could not be started.", it)
                }
        }
    }

    override fun disableLocationUpdates() {
        // Remove all geofences.
        geofencingClient.deleteGeofenceList(geofencingPendingIntent)
            .addOnSuccessListener { NotificareLogger.debug("Removed all geofences.") }
            .addOnFailureListener { NotificareLogger.debug("Failed to remove all geofences.") }

        // Stop listening for location updates.
        fusedLocationClient.removeLocationUpdates(locationPendingIntent)

        locationUpdatesStarted = false
    }

    override fun getCurrentLocationAsync(): Deferred<Location> {
        return fusedLocationClient.lastLocation.asDeferred()
    }

    override suspend fun startMonitoringRegions(regions: List<NotificareRegion>) {
        val geofences = regions.map { region ->
            Geofence.Builder()
                .setUniqueId(region.id)
                .setRoundArea(
                    region.geometry.coordinate.latitude,
                    region.geometry.coordinate.longitude,
                    region.distance.toFloat()
                )
                .setConversions(Geofence.ENTER_GEOFENCE_CONVERSION or Geofence.EXIT_GEOFENCE_CONVERSION)
                .setValidContinueTime(Geofence.GEOFENCE_NEVER_EXPIRE)
                .setNotificationInterval(Notificare.DEFAULT_GEOFENCE_RESPONSIVENESS)
                .build()
        }

        val request = GeofenceRequest.Builder()
            .createGeofenceList(geofences)
            .setInitConversions(
                GeofenceRequest.ENTER_INIT_CONVERSION
                    or GeofenceRequest.DWELL_INIT_CONVERSION
                    or GeofenceRequest.EXIT_INIT_CONVERSION
            )
            .build()

        try {
            geofencingClient.createGeofenceList(request, geofencingPendingIntent)
                .asDeferred()
                .await()

            NotificareLogger.debug("Successfully started monitoring ${geofences.size} geofences.")
        } catch (e: Exception) {
            NotificareLogger.error("Failed to start monitoring ${geofences.size} geofences.", e)
        }
    }

    override suspend fun stopMonitoringRegions(regions: List<NotificareRegion>) {
        try {
            geofencingClient.deleteGeofenceList(regions.map { it.id })
                .asDeferred()
                .await()

            NotificareLogger.debug("Successfully stopped monitoring ${regions.size} geofences.")
        } catch (e: Exception) {
            NotificareLogger.error("Failed to stop monitoring ${regions.size} geofences.", e)
        }
    }

    override suspend fun clearMonitoringRegions() {
        try {
            geofencingClient.deleteGeofenceList(geofencingPendingIntent)
                .asDeferred()
                .await()

            NotificareLogger.debug("Successfully stopped monitoring all geofences.")
        } catch (e: Exception) {
            NotificareLogger.error("Failed to stop monitoring all geofences.", e)
        }
    }
}
