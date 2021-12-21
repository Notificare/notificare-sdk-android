package re.notifica.geo.internal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import kotlinx.coroutines.*
import okhttp3.Response
import re.notifica.*
import re.notifica.geo.NotificareGeo
import re.notifica.geo.NotificareInternalGeo
import re.notifica.geo.NotificareLocationHardwareUnavailableException
import re.notifica.geo.internal.network.push.*
import re.notifica.geo.internal.storage.LocalStorage
import re.notifica.geo.ktx.logBeaconSession
import re.notifica.geo.ktx.logRegionSession
import re.notifica.geo.ktx.loyaltyIntegration
import re.notifica.geo.models.*
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.modules.integrations.NotificareGeoIntegration
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.models.NotificareApplication
import java.util.*

internal object NotificareGeoImpl : NotificareModule(), NotificareGeo, NotificareInternalGeo, NotificareGeoIntegration {

    private lateinit var localStorage: LocalStorage
    private var geocoder: Geocoder? = null
    private var serviceManager: ServiceManager? = null
    private var beaconServiceManager: BeaconServiceManager? = null
    private var lastKnownLocation: Location? = null
    private val listeners = mutableListOf<NotificareGeo.Listener>()

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

    private val locationServicesAuthStatus: String
        get() {
            return when {
                hasBackgroundLocationPermission -> "always"
                hasForegroundLocationPermission -> "use"
                else -> "none"
            }
        }

    private val locationServicesAccuracyAuth: String
        get() {
            return when {
                hasForegroundLocationPermission && !hasPreciseLocationPermission -> "reduced"
                else -> "full"
            }
        }

    private val hasBeaconSupport: Boolean
        get() {
            val context = Notificare.requireContext()

            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                NotificareLogger.warning("Beacons functionality requires Bluetooth LE.")
                return false
            }

            if (!hasBluetoothPermission) {
                NotificareLogger.warning("Beacons functionality requires bluetooth permission.")
                return false
            }

            if (!hasBluetoothScanPermission) {
                NotificareLogger.warning("Beacons functionality requires bluetooth scan permission.")
                return false
            }

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            if (bluetoothManager?.adapter?.isEnabled != true) {
                NotificareLogger.warning("Beacons functionality requires the bluetooth adapter to be enabled.")
                return false
            }

            if (Notificare.application?.regionConfig?.proximityUUID == null) {
                NotificareLogger.warning("Beacons functionality required the application to be configured with the Proximity UUID.")
                return false
            }

            if (beaconServiceManager == null) {
                NotificareLogger.warning("Beacons functionality requires the notificare-geo-beacons peer module.")
                return false
            }

            return true
        }

    // region Notificare Module

    override fun configure() {
        val context = Notificare.requireContext()

        localStorage = LocalStorage(context)
        geocoder = if (Geocoder.isPresent()) Geocoder(context) else null
        serviceManager = ServiceManager.create()
    }

    override suspend fun launch() {
        beaconServiceManager = BeaconServiceManager.create()

        if (beaconServiceManager == null) {
            NotificareLogger.info("To enable beacon support, include the notificare-geo-beacons peer dependency.")
        }
    }

    // endregion

    // region Notificare Geo

    override var hasLocationServicesEnabled: Boolean
        get() {
            if (::localStorage.isInitialized) {
                return localStorage.locationServicesEnabled
            }

            NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
            return false
        }
        private set(value) {
            localStorage.locationServicesEnabled = value
        }

    override var hasBluetoothEnabled: Boolean
        get() {
            if (::localStorage.isInitialized) {
                return localStorage.bluetoothEnabled
            }

            NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
            return false
        }
        private set(value) {
            localStorage.bluetoothEnabled = value
        }

    override fun enableLocationUpdates() {
        try {
            checkPrerequisites()
        } catch (e: Exception) {
            return
        }

        // Ensure we keep the bluetooth state updated in the API.
        updateBluetoothState(hasBeaconSupport)

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

        if (hasBeaconSupport) {
            val enteredRegions =
                localStorage.monitoredRegions.values.filter { localStorage.enteredRegions.contains(it.id) }
            if (enteredRegions.size > 1) {
                NotificareLogger.warning("Detected multiple entered regions. Beacon monitoring is limited to a single region at a time.")
            }

            val region = enteredRegions.firstOrNull()
            if (region != null) startMonitoringBeacons(region)
        }

        NotificareLogger.info("Location updates enabled.")
    }

    override fun disableLocationUpdates() {
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

        // Ensure we keep the bluetooth state updated in the API.
        updateBluetoothState(hasBeaconSupport)

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

    override fun addListener(listener: NotificareGeo.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: NotificareGeo.Listener) {
        listeners.remove(listener)
    }

    // endregion

    // region Notificare Internal Geo

    override fun handleLocationUpdate(location: Location) {
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
            .values
            .filter { it.isPolygon }
            .forEach { region ->
                val entered = localStorage.enteredRegions.contains(region.id)
                val inside = region.contains(location)

                if (!entered && inside) {
                    triggerRegionEnter(region)
                    startRegionSession(region)

                    listeners.forEach { it.onRegionEntered(region) }
                } else if (entered && !inside) {
                    triggerRegionExit(region)
                    stopRegionSession(region)

                    listeners.forEach { it.onRegionExited(region) }
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

        listeners.forEach { it.onLocationUpdated(NotificareLocation(location)) }

        Notificare.loyaltyIntegration()?.onPassbookLocationRelevanceChanged()
    }

    override fun handleRegionEnter(identifiers: List<String>) {
        identifiers.forEach { regionId ->
            val region = localStorage.monitoredRegions[regionId] ?: run {
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

                                // Start monitoring for beacons in this region.
                                startMonitoringBeacons(region)

                                listeners.forEach { it.onRegionEntered(region) }
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

                listeners.forEach { it.onRegionEntered(region) }
            }

            // Start monitoring for beacons in this region.
            startMonitoringBeacons(region)
        }
    }

    override fun handleRegionExit(identifiers: List<String>) {
        identifiers.forEach { regionId ->
            val region = localStorage.monitoredRegions[regionId] ?: run {
                NotificareLogger.warning("Received an exit event for non-cached region '$regionId'.")
                return@forEach
            }

            // Make sure we're inside the region.
            if (localStorage.enteredRegions.contains(regionId)) {
                triggerRegionExit(region)
                stopRegionSession(region)

                listeners.forEach { it.onRegionExited(region) }
            }

            // Stop monitoring for beacons in this region.
            stopMonitoringBeacons(region)
        }
    }

    override fun handleBeaconEnter(uniqueId: String, major: Int, minor: Int?) {
        val beacon = localStorage.monitoredBeacons.firstOrNull { it.id == uniqueId } ?: run {
            NotificareLogger.warning("Received a beacon enter event for non-cached beacon '$uniqueId'.")
            return
        }

        if (minor == null) {
            // This is the main region. There's no minor.
            startBeaconSession(beacon)
        } else {
            // This is a normal beacon.
            // Make sure we're not inside the beacon region.
            if (!localStorage.enteredBeacons.contains(beacon.id)) {
                triggerBeaconEnter(beacon)
            }
        }

        listeners.forEach { it.onBeaconEntered(beacon) }

        Notificare.loyaltyIntegration()?.onPassbookLocationRelevanceChanged()
    }

    override fun handleBeaconExit(uniqueId: String, major: Int, minor: Int?) {
        val beacon = localStorage.monitoredBeacons.firstOrNull { it.id == uniqueId } ?: run {
            NotificareLogger.warning("Received a beacon exit event for non-cached beacon '$uniqueId'.")
            return
        }

        if (minor == null) {
            // This is the main region. There's no minor.
            stopBeaconSession(beacon)
        } else {
            // This is a normal beacon.
            // Make sure we're inside the beacon region.
            if (localStorage.enteredBeacons.contains(beacon.id)) {
                triggerBeaconExit(beacon)
            }
        }

        listeners.forEach { it.onBeaconExited(beacon) }

        Notificare.loyaltyIntegration()?.onPassbookLocationRelevanceChanged()
    }

    override fun handleRangingBeacons(regionId: String, beacons: List<BeaconServiceManager.Beacon>) {
        val region = localStorage.monitoredRegions[regionId] ?: run {
            NotificareLogger.warning("Received a ranging beacons event for non-cached region '$regionId'.")
            return
        }

        beacons
            .map { b ->
                val beacon = localStorage.monitoredBeacons.firstOrNull { it.major == b.major && it.minor == b.minor }
                    ?: run {
                        NotificareLogger.warning("Received a ranging beacons event for non-cached beacon '${b.major}:${b.minor}'.")
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
            .also { ncBeacons ->
                updateBeaconSessions(ncBeacons)

                listeners.forEach { it.onBeaconsRanged(region, ncBeacons) }
            }
    }

    // endregion

    // region Notificare Geo Integration

    override val geoLastKnownLocation: Location?
        get() = lastKnownLocation

    override val geoEnteredBeacons: List<NotificareGeoIntegration.Beacon>
        get() {
            if (!::localStorage.isInitialized) return emptyList()

            return localStorage.enteredBeacons
                .mapNotNull { id -> localStorage.monitoredBeacons.firstOrNull { it.id == id } }
                .map { beacon -> NotificareGeoIntegration.Beacon(beacon.major, beacon.minor) }
        }

    // endregion

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.LOCATION_SERVICES] != true) {
            NotificareLogger.warning("Notificare location functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.LOCATION_SERVICES)
        }

        if (!Notificare.requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)) {
            NotificareLogger.warning("Location functionality requires location hardware.")
            throw NotificareLocationHardwareUnavailableException()
        }
    }

    private fun shouldUpdateLocation(location: Location): Boolean {
        if (lastKnownLocation == null) return true
        return location.distanceTo(lastKnownLocation) > NotificareInternalGeo.DEFAULT_LOCATION_UPDATES_SMALLEST_DISPLACEMENT
    }

    private suspend fun updateLocation(location: Location, country: String?): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice ?: run {
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
        val device = Notificare.device().currentDevice ?: run {
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

    private fun updateBluetoothState(enabled: Boolean) {
        if (hasBluetoothEnabled == enabled) return

        val device = Notificare.device().currentDevice ?: run {
            NotificareLogger.warning("Cannot update the bluetooth state without a device.")
            return
        }

        val payload = UpdateBluetoothPayload(
            bluetoothEnabled = enabled,
        )

        NotificareRequest.Builder()
            .put("/device/${device.id}", payload)
            .response(object : NotificareCallback<Response> {
                override fun onSuccess(result: Response) {
                    NotificareLogger.debug("Bluetooth state updated.")
                    hasBluetoothEnabled = enabled
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.error("Failed to update the bluetooth state.", e)
                }
            })
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
            .values
            .filter { monitoredRegion -> !regions.any { it.id == monitoredRegion.id } }
            .onEach { NotificareLogger.debug("Stopped monitoring region '${it.name}'.") }
            .also { staleRegions ->
                if (staleRegions.isEmpty()) return@also

                // Make sure we process the region exit appropriately.
                // This should perform the exit trigger, stop the session
                // and stop monitoring for beacons in this region.

                NotificareLogger.debug("Stopped monitoring ${staleRegions.size} regions.")
                serviceManager?.stopMonitoringRegions(staleRegions)
                handleRegionExit(staleRegions.map { r -> r.id })

                // Remove the regions from the cache.
                localStorage.monitoredRegions = localStorage.monitoredRegions.toMutableMap().apply {
                    staleRegions.forEach {
                        remove(it.id)
                    }
                }
            }

        // Process which regions should be monitored.
        regions
            .onEach { NotificareLogger.debug("Started monitoring region '${it.name}'.") }
            .also { freshRegions ->
                if (freshRegions.isEmpty()) return@also

                NotificareLogger.debug("Started monitoring ${freshRegions.size} regions.")
                serviceManager?.startMonitoringRegions(freshRegions)

                // Add the regions to the cache.
                localStorage.monitoredRegions = localStorage.monitoredRegions.toMutableMap().apply {
                    freshRegions.forEach {
                        put(it.id, it)
                    }
                }
            }
    }

    private fun startMonitoringBeacons(region: NotificareRegion) {
        if (!hasBeaconSupport) return

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

                    val mainBeacon = NotificareBeacon(
                        id = region.id,
                        name = region.name,
                        major = region.major,
                        minor = null,
                    )

                    // Keep track of the beacons being monitored.
                    localStorage.monitoredBeacons = beacons + mainBeacon

                    // Start monitoring them.
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
        val device = Notificare.device().currentDevice ?: run {
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
        val device = Notificare.device().currentDevice ?: run {
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
        val device = Notificare.device().currentDevice ?: run {
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
                    localStorage.enteredBeacons = localStorage.enteredBeacons + beacon.id
                    NotificareLogger.debug("Triggered beacon enter.")
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.error("Failed to trigger a beacon enter.", e)
                }
            })
    }

    private fun triggerBeaconExit(beacon: NotificareBeacon) {
        val device = Notificare.device().currentDevice ?: run {
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
                    localStorage.enteredBeacons = localStorage.enteredBeacons - beacon.id
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
        Notificare.events().logRegionSession(session, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {}

            override fun onFailure(e: Exception) {}
        })

        // Remove the session from local storage.
        localStorage.removeRegionSession(session)
    }

    private fun startBeaconSession(beacon: NotificareBeacon) {
        val region = localStorage.monitoredRegions[beacon.id] ?: run {
            NotificareLogger.warning("Cannot start the session for beacon '${beacon.name}' since the corresponding region is not being monitored.")
            return
        }

        NotificareLogger.debug("Starting session for beacon '${beacon.name}'.")
        localStorage.addBeaconSession(
            NotificareBeaconSession(
                regionId = region.id,
                start = Date(),
                end = null,
                beacons = mutableListOf(),
            )
        )
    }

    private fun updateBeaconSessions(beacons: List<NotificareBeacon>) {
        localStorage.updateBeaconSession(beacons, lastKnownLocation)
    }

    private fun stopBeaconSession(beacon: NotificareBeacon) {
        val region = localStorage.monitoredRegions[beacon.id] ?: run {
            NotificareLogger.warning("Cannot start the session for beacon '${beacon.name}' since the corresponding region is not being monitored.")
            return
        }

        val session = localStorage.beaconSessions[region.id] ?: run {
            NotificareLogger.warning("Skipping beacon session end since no session exists for region '${region.name}'.")
            return
        }

        // Submit the event for processing.
        Notificare.events().logBeaconSession(session, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {}

            override fun onFailure(e: Exception) {}
        })

        // Remove the session from local storage.
        localStorage.removeBeaconSession(session)
    }

    private fun clearRegions() {
        // Remove the cached regions.
        localStorage.monitoredRegions = emptyMap()
        localStorage.enteredRegions = emptySet()
        localStorage.clearRegionSessions()

        // Stop monitoring all regions.
        serviceManager?.clearMonitoringRegions()
    }

    private fun clearBeacons() {
        // Remove the cached beacons.
        localStorage.monitoredBeacons = emptyList()
        localStorage.enteredBeacons = emptySet()
        localStorage.clearBeaconSessions()

        // Stop monitoring all beacons.
        beaconServiceManager?.clearMonitoring()
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
}