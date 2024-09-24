package re.notifica.geo.gms.internal

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.annotation.Keep
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.geo.gms.LocationReceiver
import re.notifica.geo.internal.ServiceManager
import re.notifica.geo.internal.logger
import re.notifica.geo.ktx.DEFAULT_GEOFENCE_RESPONSIVENESS
import re.notifica.geo.ktx.DEFAULT_LOCATION_UPDATES_FASTEST_INTERVAL
import re.notifica.geo.ktx.DEFAULT_LOCATION_UPDATES_INTERVAL
import re.notifica.geo.ktx.DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT
import re.notifica.geo.ktx.INTENT_ACTION_GEOFENCE_TRANSITION
import re.notifica.geo.ktx.INTENT_ACTION_INTERNAL_LOCATION_UPDATED
import re.notifica.geo.models.NotificareRegion

@Keep
@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    private val fusedLocationClient: FusedLocationProviderClient
    private val geofencingClient: GeofencingClient

    private var locationUpdatesStarted = false
    private val locationPendingIntent: PendingIntent
    private val geofencingPendingIntent: PendingIntent

    override val available: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    init {
        val context = Notificare.requireContext()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        geofencingClient = LocationServices.getGeofencingClient(context)

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

    @SuppressLint("MissingPermission")
    override fun enableLocationUpdates() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // NotificareGeo.handleLocationUpdate(location)

                val intent = Intent()
                    .putExtra(
                        "com.google.android.gms.location.EXTRA_LOCATION_RESULT",
                        LocationResult.create(arrayListOf(location))
                    )

                try {
                    logger.info("Sending current location as an update intent.")
                    locationPendingIntent.send(Notificare.requireContext(), 0, intent)
                } catch (e: Exception) {
                    logger.error("Error sending location update broadcast.", e)
                }
            } else {
                logger.warning("No location found yet.")
            }

            if (locationUpdatesStarted) {
                logger.debug("Location updates were previously enabled. Skipping...")
                return@addOnSuccessListener
            }

            val request = LocationRequest.Builder(Notificare.DEFAULT_LOCATION_UPDATES_INTERVAL)
                .setMinUpdateIntervalMillis(Notificare.DEFAULT_LOCATION_UPDATES_FASTEST_INTERVAL)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMinUpdateDistanceMeters(Notificare.DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT.toFloat())
                .build()

            fusedLocationClient.requestLocationUpdates(request, locationPendingIntent)
                .addOnSuccessListener {
                    logger.info("Location updates started.")
                    locationUpdatesStarted = true
                }
                .addOnFailureListener {
                    logger.error("Location updates could not be started.", it)
                }
        }
    }

    override fun disableLocationUpdates() {
        // Remove all geofences.
        geofencingClient.removeGeofences(geofencingPendingIntent)
            .addOnSuccessListener { logger.debug("Removed all geofences.") }
            .addOnFailureListener { logger.debug("Failed to remove all geofences.") }

        // Stop listening for location updates.
        fusedLocationClient.removeLocationUpdates(locationPendingIntent)

        locationUpdatesStarted = false
    }

    @SuppressLint("MissingPermission")
    override fun getCurrentLocationAsync(): Deferred<Location> {
        return fusedLocationClient.lastLocation.asDeferred()
    }

    @SuppressLint("MissingPermission")
    override suspend fun startMonitoringRegions(regions: List<NotificareRegion>) {
        val geofences = regions.map { region ->
            Geofence.Builder()
                .setRequestId(region.id)
                .setCircularRegion(
                    region.geometry.coordinate.latitude,
                    region.geometry.coordinate.longitude,
                    region.distance.toFloat()
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setNotificationResponsiveness(Notificare.DEFAULT_GEOFENCE_RESPONSIVENESS)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .addGeofences(geofences)
            .setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER
                    or GeofencingRequest.INITIAL_TRIGGER_DWELL
                    or GeofencingRequest.INITIAL_TRIGGER_EXIT
            )
            .build()

        try {
            geofencingClient.addGeofences(request, geofencingPendingIntent)
                .await()

            logger.debug("Successfully started monitoring ${geofences.size} geofences.")
        } catch (e: Exception) {
            logger.error("Failed to start monitoring ${geofences.size} geofences.", e)
        }
    }

    override suspend fun stopMonitoringRegions(regions: List<NotificareRegion>) {
        try {
            geofencingClient.removeGeofences(regions.map { it.id }).await()
            logger.debug("Successfully stopped monitoring ${regions.size} geofences.")
        } catch (e: Exception) {
            logger.error("Failed to stop monitoring ${regions.size} geofences.", e)
        }
    }

    override suspend fun clearMonitoringRegions() {
        try {
            geofencingClient.removeGeofences(geofencingPendingIntent)
            logger.debug("Successfully stopped monitoring all geofences.")
        } catch (e: Exception) {
            logger.error("Failed to stop monitoring all geofences.", e)
        }
    }
}
