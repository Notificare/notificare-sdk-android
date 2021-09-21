package re.notifica.geo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import kotlinx.coroutines.*
import okhttp3.Response
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareException
import re.notifica.geo.internal.ServiceManager
import re.notifica.geo.internal.contains
import re.notifica.geo.internal.isValid
import re.notifica.geo.internal.network.push.FetchRegionsResponse
import re.notifica.geo.internal.network.push.RegionTriggerPayload
import re.notifica.geo.internal.network.push.UpdateDeviceLocationPayload
import re.notifica.geo.internal.storage.LocalStorage
import re.notifica.geo.models.NotificareRegion
import re.notifica.internal.NotificareLogger
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.modules.NotificareModule

public object NotificareGeo : NotificareModule() {

    // TODO consider providing these parameters as options.
    public const val DEFAULT_LOCATION_UPDATES_INTERVAL: Long = 60 * 1000
    public const val DEFAULT_LOCATION_UPDATES_FASTEST_INTERVAL: Long = 30 * 1000
    public const val DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT: Double = 10.0
    public const val DEFAULT_GEOFENCE_RESPONSIVENESS: Int = 0

    public const val INTENT_ACTION_LOCATION_UPDATED: String = "re.notifica.intent.action.LocationUpdated"
    public const val INTENT_ACTION_GEOFENCE_TRANSITION: String = "re.notifica.intent.action.GeofenceTransition"

    private lateinit var localStorage: LocalStorage
    private var serviceManager: ServiceManager? = null
    private var lastKnownLocation: Location? = null

    private val hasForegroundLocationPermission: Boolean
        get() {
            return if (BuildCompat.isAtLeastS()) {
                ContextCompat.checkSelfPermission(
                    Notificare.requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    Notificare.requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

    private val hasBackgroundLocationPermission: Boolean
        get() {
            val hasBackgroundAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    Notificare.requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            return hasBackgroundAccess && hasForegroundLocationPermission
        }

    private val hasPreciseLocationPermission: Boolean
        get() {
            return ContextCompat.checkSelfPermission(
                Notificare.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

    public val locationServicesEnabled: Boolean
        get() = true

    // region NotificareModule

    override fun configure() {
        localStorage = LocalStorage(Notificare.requireContext())
        serviceManager = ServiceManager.create()
    }

    override suspend fun launch() {}

    override suspend fun unlaunch() {}

    // endregion

    internal val locationServicesAuthStatus: String
        get() {
            return when {
                hasBackgroundLocationPermission -> "always"
                hasForegroundLocationPermission -> "use"
                else -> "none"
            }
        }

    internal val locationServicesAccuracyAuth: String
        get() {
            return when {
                hasForegroundLocationPermission && !hasPreciseLocationPermission -> "reduced"
                else -> "full"
            }
        }


    public fun enableLocationUpdates() {
        try {
            checkPrerequisites()
        } catch (e: Exception) {
            return
        }

        if (!hasForegroundLocationPermission) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                try {
                    clearLocation()
                    NotificareLogger.debug("Device location cleared.")

                    clearRegions()
                    // TODO: clearBeacons()
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to clear the device location.", e)
                }
            }

            return
        }

        serviceManager?.enableLocationUpdates()
    }

    public fun disableLocationUpdates() {
        try {
            checkPrerequisites()
        } catch (e: Exception) {
            return
        }

        lastKnownLocation = null
        clearRegions()
        // TODO clear beacons

        serviceManager?.disableLocationUpdates()

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            try {
                clearLocation()
                NotificareLogger.debug("Device location cleared.")
            } catch (e: Exception) {
                NotificareLogger.error("Failed to clear the device location.", e)
            }
        }
    }


    @InternalNotificareApi
    public fun handleLocationUpdate(location: Location) {
        if (!location.isValid) {
            NotificareLogger.warning("Received an invalid location update(${location.latitude}, ${location.longitude}).")
            return
        }

        //
        // Handle ongoing region sessions.
        //
        // TODO handle region sessions

        //
        // Handle polygon enters & exits.
        //
        localStorage.monitoredRegions
            .filter { it.isPolygon }
            .forEach { region ->
                val entered = localStorage.enteredRegions.contains(region.id)
                val inside = region.contains(location)

                if (!entered && inside) {
                    triggerRegionEnter(region)

                    // TODO start region session
                } else if (entered && !inside) {
                    triggerRegionExit(region)

                    // TODO stop region session
                }

                if (inside) {
                    // TODO start monitoring for beacons
                } else {
                    // TODO stop monitoring for beacons
                }
            }

        //
        // Handle location updates & loading new geofences.
        //
        if (shouldUpdateLocation(location)) {
            Toast.makeText(Notificare.requireContext(), "location updated", Toast.LENGTH_SHORT).show()

            // Keep a reference to the last known location.
            lastKnownLocation = location

            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                try {
                    val country = checkNotNull(serviceManager).getCountryCode(location)

                    NotificareLogger.info("Updating device location.")
                    updateLocation(location, country)

                    NotificareLogger.debug("Loading nearest regions.")
                    loadNearestRegions(location)
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to process a location update.", e)
                }
            }
        }
    }

    @InternalNotificareApi
    public fun handleRegionEnter(identifiers: List<String>) {
        Toast.makeText(Notificare.requireContext(), "on enter = $identifiers", Toast.LENGTH_LONG).show()

        identifiers.forEach { regionId ->
            val region = localStorage.monitoredRegions.firstOrNull { it.id == regionId } ?: run {
                NotificareLogger.warning("Received an enter event for non-cached region '$regionId'.")
                return@forEach
            }

            if (region.isPolygon) {
                NotificareLogger.debug("Handling polygon region (${region.name}).")

                if (!localStorage.enteredRegions.contains(regionId)) {
                    @OptIn(DelicateCoroutinesApi::class)
                    GlobalScope.launch {
                        try {
                            val location = checkNotNull(serviceManager).getCurrentLocationAsync().await()
                            val inside = region.contains(location)

                            if (inside) {
                                NotificareLogger.debug("Entered the polygon.")

                                triggerRegionEnter(region)
                                // TODO start region session
                            }
                        } catch (e: Exception) {
                            NotificareLogger.warning("Failed to determine the current location.", e)
                        }
                    }
                } else {
                    // Start monitoring for beacons in this region.
                    // startMonitoringBeacons(region)
                }

                return@forEach
            }

            // Make sure we're not inside the region.
            if (!localStorage.enteredRegions.contains(regionId)) {
                triggerRegionEnter(region)
                // TODO start region session
            }

            // Start monitoring for beacons in this region.
            // startMonitoringBeacons(region)
        }
    }

    @InternalNotificareApi
    public fun handleRegionExit(identifiers: List<String>) {
        Toast.makeText(Notificare.requireContext(), "on exit = $identifiers", Toast.LENGTH_LONG).show()

        identifiers.forEach { regionId ->
            val region = localStorage.monitoredRegions.firstOrNull { it.id == regionId } ?: run {
                NotificareLogger.warning("Received an exit event for non-cached region '$regionId'.")
                return@forEach
            }

            // Make sure we're inside the region.
            if (localStorage.enteredRegions.contains(regionId)) {
                triggerRegionExit(region)
                // TODO end region session
            }

            // Stop monitoring for beacons in this region.
            // stopMonitoringBeacons(region)
        }
    }


    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareException.NotReady()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareException.NotReady()
        }

        if (application.services["locationServices"] != true) {
            NotificareLogger.warning("Notificare location functionality is not enabled.")
            throw NotificareException.NotReady()
        }

        if (!Notificare.requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)) {
            NotificareLogger.warning("Location functionality requires location hardware.")
            throw NotificareException.NotReady()
        }
    }

    private fun shouldUpdateLocation(location: Location): Boolean {
        if (lastKnownLocation == null) return true
        return location.distanceTo(lastKnownLocation) > DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT
    }

    private suspend fun updateLocation(location: Location, country: String?): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.deviceManager.currentDevice ?: run {
            NotificareLogger.warning("Unable to update location without a device.")
            throw IllegalStateException("Unable to update location without a device.")
        }

        val payload = UpdateDeviceLocationPayload(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            locationAccuracy = location.accuracy.toDouble(),
            speed = location.speed.toDouble(),
            course = location.bearing.toDouble(),
            country = country,
            // floor = null,
            locationServicesAuthStatus = locationServicesAuthStatus,
            locationServicesAccuracyAuth = locationServicesAccuracyAuth,
        )

        NotificareRequest.Builder()
            .put("/device/${device.id}", payload)
            .response()
    }

    private suspend fun clearLocation(): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.deviceManager.currentDevice ?: run {
            throw IllegalStateException("Cannot update location authorization state without a device.")
        }

        val payload = UpdateDeviceLocationPayload(
            latitude = null,
            longitude = null,
            altitude = null,
            locationAccuracy = null,
            speed = null,
            course = null,
            country = null,
            // floor = null,
            locationServicesAuthStatus = null,
            locationServicesAccuracyAuth = null,
        )

        NotificareRequest.Builder()
            .put("/device/${device.id}", payload)
            .response()
    }

    private suspend fun loadNearestRegions(location: Location): Unit = withContext(Dispatchers.IO) {
        try {
            val regions = NotificareRequest.Builder()
                .get("/region/bylocation/${location.latitude}/${location.longitude}")
                .responseDecodable(FetchRegionsResponse::class)
                .regions
                .map { it.toModel() }

            monitorRegions(regions)
        } catch (e: Exception) {
            NotificareLogger.error("Failed to load nearest regions.", e)
        }
    }

    private fun monitorRegions(regions: List<NotificareRegion>) {
        if (!hasBackgroundLocationPermission) {
            NotificareLogger.debug("Background location permission not granted. Skipping geofencing functionality.")
            return
        }

        if (!hasPreciseLocationPermission) {
            NotificareLogger.debug("Precise location permission not granted. Skipping geofencing functionality.")
            return
        }

        NotificareLogger.debug("Processing the region differential for monitoring.")

        // Process which regions should be removed from monitoring.
        localStorage.monitoredRegions
            .filter { monitoredRegion -> !regions.any { it.id == monitoredRegion.id } }
            .onEach { NotificareLogger.debug("Stopped monitoring region '${it.name}'.") }
            .also {
                if (it.isEmpty()) return@also

                // Make sure we process the region exit appropriately.
                // This should perform the exit trigger, stop the session
                // and stop monitoring for beacons in this region.

                NotificareLogger.debug("Stopped monitoring ${it.size} regions.")
                serviceManager?.stopMonitoringRegions(it)
                handleRegionExit(it.map { r -> r.id })

                // Remove the regions from the cache.
                localStorage.monitoredRegions = localStorage.monitoredRegions
                    .filter { region -> it.any { r -> r.id == region.id } }
            }

        // Process which regions should be monitored.
        regions
            .onEach { NotificareLogger.debug("Started monitoring region '${it.name}'.") }
            .also {
                if (it.isEmpty()) return@also

                NotificareLogger.debug("Started monitoring ${it.size} regions.")
                serviceManager?.startMonitoringRegions(it)

                // Add the regions to the cache.
                localStorage.monitoredRegions = localStorage.monitoredRegions + it
            }
    }

    private fun triggerRegionEnter(region: NotificareRegion) {
        val device = Notificare.deviceManager.currentDevice ?: run {
            NotificareLogger.warning("Cannot process region enter trigger without a device.")
            return
        }

        val payload = RegionTriggerPayload(
            deviceID = device.id,
            region = region.id,
        )

        NotificareRequest.Builder()
            .post("/trigger/re.notifica.trigger.region.Enter", payload)
            .response(object : NotificareCallback<Response> {
                override fun onSuccess(result: Response) {
                    localStorage.enteredRegions = localStorage.enteredRegions + region.id
                    NotificareLogger.debug("Triggered region enter.")
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.error("Failed to trigger a region enter.", e)
                }
            })
    }

    private fun triggerRegionExit(region: NotificareRegion) {
        val device = Notificare.deviceManager.currentDevice ?: run {
            NotificareLogger.warning("Cannot process region exit trigger without a device.")
            return
        }

        val payload = RegionTriggerPayload(
            deviceID = device.id,
            region = region.id,
        )

        NotificareRequest.Builder()
            .post("/trigger/re.notifica.trigger.region.Exit", payload)
            .response(object : NotificareCallback<Response> {
                override fun onSuccess(result: Response) {
                    localStorage.enteredRegions = localStorage.enteredRegions - region.id
                    NotificareLogger.debug("Triggered region exit.")
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.error("Failed to trigger a region exit.", e)
                }
            })
    }

    private fun clearRegions() {
        // Remove the cached regions.
        localStorage.monitoredRegions = emptyList()

        // Stop monitoring all regions.
        serviceManager?.clearMonitoringRegions()
    }
}
