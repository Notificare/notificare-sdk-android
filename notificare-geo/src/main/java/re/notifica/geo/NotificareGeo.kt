package re.notifica.geo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import okhttp3.Response
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareException
import re.notifica.geo.internal.BeaconServiceManager
import re.notifica.geo.internal.ServiceManager
import re.notifica.geo.internal.contains
import re.notifica.geo.internal.isValid
import re.notifica.geo.internal.network.push.*
import re.notifica.geo.internal.storage.LocalStorage
import re.notifica.geo.ktx.logRegionSession
import re.notifica.geo.models.*
import re.notifica.internal.NotificareLogger
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.modules.NotificareModule

public object NotificareGeo : NotificareModule() {

    // TODO consider providing these parameters as options.
    public const val DEFAULT_LOCATION_UPDATES_INTERVAL: Long = (60 * 1000).toLong()
    public const val DEFAULT_LOCATION_UPDATES_FASTEST_INTERVAL: Long = (30 * 1000).toLong()
    public const val DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT: Double = 10.0
    public const val DEFAULT_GEOFENCE_RESPONSIVENESS: Int = 0

    public const val INTENT_ACTION_LOCATION_UPDATED: String = "re.notifica.intent.action.LocationUpdated"
    public const val INTENT_ACTION_GEOFENCE_TRANSITION: String = "re.notifica.intent.action.GeofenceTransition"

    private lateinit var localStorage: LocalStorage
    private var geocoder: Geocoder? = null
    private var serviceManager: ServiceManager? = null
    private var beaconServiceManager: BeaconServiceManager? = null
    private var lastKnownLocation: Location? = null
    private val listeners = mutableListOf<Listener>()

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

    private val hasBluetoothPermission: Boolean
        get() {
            return ContextCompat.checkSelfPermission(
                Notificare.requireContext(),
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }

    private val hasBluetoothScanPermission: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    Notificare.requireContext(),
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

    public var locationServicesEnabled: Boolean
        get() = localStorage.locationServicesEnabled
        private set(value) {
            localStorage.locationServicesEnabled = value
        }

    private val _rangingBeacons = MutableLiveData<List<NotificareBeacon>>(emptyList())
    public val rangingBeacons: LiveData<List<NotificareBeacon>> = _rangingBeacons

    // region NotificareModule

    override fun configure() {
        val context = Notificare.requireContext()

        localStorage = LocalStorage(context)
        geocoder = if (Geocoder.isPresent()) Geocoder(context) else null
        serviceManager = ServiceManager.create()
        beaconServiceManager = BeaconServiceManager.create()
        // TODO cannot create this during configuration as the application may not be available yet.

        if (beaconServiceManager == null) {
            NotificareLogger.info("To enable beacon support, include the notificare-geo-beacons peer dependency.")
        }
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
                    lastKnownLocation = null
                    clearLocation()
                    NotificareLogger.debug("Device location cleared.")

                    clearRegions()
                    clearBeacons()
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to clear the device location.", e)
                }
            }

            return
        }

        // Keep track of the location services status.
        localStorage.locationServicesEnabled = true

        // Start the location updates.
        serviceManager?.enableLocationUpdates()

        NotificareLogger.info("Location updates enabled.")
    }

    public fun disableLocationUpdates() {
        try {
            checkPrerequisites()
        } catch (e: Exception) {
            return
        }

        // Keep track of the location services status.
        localStorage.locationServicesEnabled = false

        lastKnownLocation = null
        clearRegions()
        clearBeacons()

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

        NotificareLogger.info("Location updates disabled.")
    }

    public fun enableBeaconUpdates() {
        try {
            checkPrerequisites()
            checkBeaconPrerequisites()
        } catch (e: Exception) {
            return
        }
    }

    public fun disableBeaconUpdates() {

    }

    public fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    public fun removeListener(listener: Listener) {
        listeners.remove(listener)
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
        updateRegionSessions(NotificareLocation(location))

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
                    startRegionSession(region)
                } else if (entered && !inside) {
                    triggerRegionExit(region)
                    stopRegionSession(region)
                }

                if (inside) {
                    startMonitoringBeacons(region)
                } else {
                    stopMonitoringBeacons(region)
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
                    val country = getCountryCode(location)

                    NotificareLogger.info("Updating device location.")
                    updateLocation(location, country)

                    NotificareLogger.debug("Loading nearest regions.")
                    loadNearestRegions(location)
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to process a location update.", e)
                }
            }
        }

        // TODO emit a location broadcast so the loyalty module can pick it up for relevance notifications.

        listeners.forEach { it.onLocationUpdated(NotificareLocation(location)) }
    }

    @InternalNotificareApi
    public fun handleRegionEnter(identifiers: List<String>) {
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
                                startRegionSession(region)
                            }
                        } catch (e: Exception) {
                            NotificareLogger.warning("Failed to determine the current location.", e)
                        }
                    }
                } else {
                    // Start monitoring for beacons in this region.
                    startMonitoringBeacons(region)
                }

                return@forEach
            }

            // Make sure we're not inside the region.
            if (!localStorage.enteredRegions.contains(regionId)) {
                triggerRegionEnter(region)
                startRegionSession(region)
            }

            // Start monitoring for beacons in this region.
            startMonitoringBeacons(region)
        }
    }

    @InternalNotificareApi
    public fun handleRegionExit(identifiers: List<String>) {
        identifiers.forEach { regionId ->
            val region = localStorage.monitoredRegions.firstOrNull { it.id == regionId } ?: run {
                NotificareLogger.warning("Received an exit event for non-cached region '$regionId'.")
                return@forEach
            }

            // Make sure we're inside the region.
            if (localStorage.enteredRegions.contains(regionId)) {
                triggerRegionExit(region)
                stopRegionSession(region)
            }

            // Stop monitoring for beacons in this region.
            stopMonitoringBeacons(region)
        }
    }

    @InternalNotificareApi
    public fun handleBeaconEnter(uniqueId: String, major: Int, minor: Int?) {
        if (minor == null) {
            // This is the main region. There's no minor.

            val region = localStorage.monitoredRegions.firstOrNull { it.id == uniqueId } ?: run {
                NotificareLogger.warning("Received a beacon enter event for non-cached region '$uniqueId'.")
                return
            }

            // TODO start beacon session

            return
        }

        val beacon = localStorage.monitoredBeacons.firstOrNull { it.id == uniqueId } ?: run {
            NotificareLogger.warning("Received a beacon enter event for non-cached beacon '$uniqueId'.")
            return
        }

        // TODO check where to update the beacon session.
    }

    @InternalNotificareApi
    public fun handleBeaconExit(uniqueId: String, major: Int, minor: Int?) {

    }

    @InternalNotificareApi
    public fun handleRangingBeacons(regionId: String, beacons: List<BeaconServiceManager.Beacon>) {
        val region = localStorage.monitoredRegions.firstOrNull { it.id == regionId } ?: run {
            NotificareLogger.warning("non cached")
            return
        }

        val beacons: List<NotificareBeacon> = beacons
            .map { b ->
                val beacon =
                    localStorage.monitoredBeacons.firstOrNull { it.major == b.major && it.minor == b.minor } ?: run {
                        NotificareLogger.warning("non cached")
                        return@map null
                    }

                if (b.proximity == null || b.proximity < 0) {
                    // Ignore invalid proximity values.
                    return@map null
                }

                beacon.proximity = when {
                    b.proximity < NotificareBeacon.PROXIMITY_NEAR_DISTANCE -> NotificareBeacon.Proximity.IMMEDIATE
                    b.proximity < NotificareBeacon.PROXIMITY_FAR_DISTANCE -> NotificareBeacon.Proximity.NEAR
                    else -> NotificareBeacon.Proximity.FAR
                }

                beacon
            }
            .filterNotNull()

        listeners.forEach { it.onBeaconsRanged(region, beacons) }
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

    @Throws
    private fun checkBeaconPrerequisites() {
        if (!Notificare.requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            NotificareLogger.warning("Beacons functionality requires Bluetooth LE.")
            throw NotificareException.NotReady()
        }

        // TODO consider checking the permissions elsewhere

        if (!hasBluetoothPermission) {
            NotificareLogger.warning("Beacons functionality requires bluetooth permission.")
            throw NotificareException.NotReady()
        }

        if (!hasBluetoothScanPermission) {
            NotificareLogger.warning("Beacons functionality requires bluetooth scan permission.")
            throw NotificareException.NotReady()
        }

        // todo also needs to check location permissions
        // todo also check the proximityUUID for the cached application

        if (beaconServiceManager == null) {
            NotificareLogger.warning("Beacons functionality requires the notificare-geo-beacons peer module.")
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

//            val beaconsPerRegion: Map<String, List<NotificareBeacon>> = regions
//                .filter { it.major != null }
//                .map { region ->
//                    val beacons = NotificareRequest.Builder()
//                        .get("beacon/forregion/${region.id}")
//                        .responseDecodable(FetchBeaconsResponse::class)
//                        .beacons
//                        .map { it.toModel() }
//
//                    region.id to beacons
//                }
//                .toMap()

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

    private fun startMonitoringBeacons(region: NotificareRegion) {
        if (region.major == null) {
            NotificareLogger.debug("The region '${region.name}' has not been assigned a major.")
            return
        }

        NotificareRequest.Builder()
            .get("/beacon/forregion/${region.id}")
            .responseDecodable(FetchBeaconsResponse::class, object : NotificareCallback<FetchBeaconsResponse> {
                override fun onSuccess(result: FetchBeaconsResponse) {
                    val beacons = result.beacons
                        .map { it.toModel() }
                        .filter { it.triggers }

                    beaconServiceManager?.startMonitoring(region, beacons)
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.error("Failed to fetch beacons for region '${region.name}'.", e)
                }
            })
    }

    private fun stopMonitoringBeacons(region: NotificareRegion) {
        beaconServiceManager?.stopMonitoring(region)
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

    private fun triggerBeaconEnter(beacon: NotificareBeacon) {
        val device = Notificare.deviceManager.currentDevice ?: run {
            NotificareLogger.warning("Cannot process beacon enter trigger without a device.")
            return
        }

        val payload = BeaconTriggerPayload(
            deviceID = device.id,
            beacon = beacon.id,
        )

        NotificareRequest.Builder()
            .post("/trigger/re.notifica.trigger.beacon.Enter", payload)
            .response(object : NotificareCallback<Response> {
                override fun onSuccess(result: Response) {
                    // TODO localStorage.enteredBeacons = localStorage.enteredBeacons + region.id
                    NotificareLogger.debug("Triggered beacon enter.")
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.error("Failed to trigger a beacon enter.", e)
                }
            })
    }

    private fun triggerBeaconExit(beacon: NotificareBeacon) {
        val device = Notificare.deviceManager.currentDevice ?: run {
            NotificareLogger.warning("Cannot process beacon exit trigger without a device.")
            return
        }

        val payload = BeaconTriggerPayload(
            deviceID = device.id,
            beacon = beacon.id,
        )

        NotificareRequest.Builder()
            .post("/trigger/re.notifica.trigger.beacon.Exit", payload)
            .response(object : NotificareCallback<Response> {
                override fun onSuccess(result: Response) {
                    // TODO localStorage.enteredRegions = localStorage.enteredRegions - region.id
                    NotificareLogger.debug("Triggered beacon exit.")
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.error("Failed to trigger a beacon exit.", e)
                }
            })
    }

    private fun startRegionSession(region: NotificareRegion) {
        NotificareLogger.debug("Starting session for region '${region.name}'.")
        val session = NotificareRegionSession(region)

        val location = lastKnownLocation?.let { NotificareLocation(it) }
        if (location != null) {
            session.locations.add(location)
        }

        localStorage.addRegionSession(session)
    }

    private fun updateRegionSessions(location: NotificareLocation) {
        NotificareLogger.debug("Updating region sessions.")
        localStorage.updateRegionSessions(location)
    }

    private fun stopRegionSession(region: NotificareRegion) {
        NotificareLogger.debug("Stopping session for region '${region.name}'.")

        val session = localStorage.regionSessions[region.id] ?: run {
            NotificareLogger.warning("Skipping region session end since no session exists for region '${region.name}'.")
            return
        }

        // Submit the event for processing.
        Notificare.eventsManager.logRegionSession(session)

        // Remove the session from local storage.
        localStorage.removeRegionSession(session)
    }

    private fun startBeaconSession(beacon: NotificareBeacon) {
        NotificareLogger.debug("Starting session for beacon '${beacon.name}'.")
        // val session = NotificareBeaconSession(region)

        val location = lastKnownLocation?.let { NotificareLocation(it) }
        if (location != null) {
            // session.locations.add(location)
        }

//        session.beacons.add(
//            NotificareBeaconSession.Beacon(
//                proximity =
//            )
//        )

        // localStorage.addRegionSession(session)
    }

    private fun updateBeaconSessions(location: NotificareLocation) {
        // TODO
    }

    private fun stopBeaconSession(beacon: NotificareBeacon) {
        // TODO
    }

    private fun clearRegions() {
        // Remove the cached regions.
        localStorage.monitoredRegions = emptyList()
        localStorage.enteredRegions = emptySet()

        // Stop monitoring all regions.
        serviceManager?.clearMonitoringRegions()
    }

    private fun clearBeacons() {
        // TODO
    }

    private fun getCountryCode(location: Location): String? {
        return try {
            geocoder
                ?.getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.countryCode
        } catch (e: Exception) {
            NotificareLogger.warning("Unable to reverse geocode the location.", e)
            null
        }
    }


    public interface Listener {
        public fun onLocationUpdated(location: NotificareLocation) {}

        public fun onEnterRegion(region: NotificareRegion) {}

        public fun onExitRegion(region: NotificareRegion) {}

        public fun onEnterBeacon(beacon: NotificareBeacon) {}

        public fun onExitBeacon(beacon: NotificareBeacon) {}

        public fun onBeaconsRanged(region: NotificareRegion, beacons: List<NotificareBeacon>) {}
    }
}
