package re.notifica.geo.beacons.internal

import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.RangedBeacon
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.geo.beacons.ktx.geoInternal
import re.notifica.geo.internal.BeaconServiceManager
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareRegion
import re.notifica.internal.NotificareLogger

private const val BEACON_LAYOUT_APPLE = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
private const val DEFAULT_SCAN_INTERVAL: Long = 30000L
private const val DEFAULT_SAMPLE_EXPIRATION: Long = 10000L

// AltBeacon.uniqueId   = NotificareRegion.id / NotificareBeacon.id
// AltBeacon.id1        = Proximity UUID
// AltBeacon.id2        = Major
// AltBeacon.id3        = Minor

@InternalNotificareApi
public class BeaconServiceManager(
    proximityUUID: String,
) : BeaconServiceManager(proximityUUID), MonitorNotifier, RangeNotifier {

    private val beaconManager: BeaconManager

    init {
        val context = Notificare.requireContext()

        beaconManager = BeaconManager.getInstanceForApplication(context)
        beaconManager.beaconParsers.clear()
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BEACON_LAYOUT_APPLE))
        beaconManager.backgroundBetweenScanPeriod = DEFAULT_SCAN_INTERVAL

        RangedBeacon.setSampleExpirationMilliseconds(DEFAULT_SAMPLE_EXPIRATION)

//        if (scanInterval > 0) {
//            beaconManager.backgroundBetweenScanPeriod = scanInterval
//        }
//
//        if (sampleExpiration > 0) {
//            RangedBeacon.setSampleExpirationMilliseconds(sampleExpiration)
//        } else {
//            RangedBeacon.setSampleExpirationMilliseconds(DEFAULT_SAMPLE_EXPIRATION)
//        }
//
//        if (useForegroundServiceScanning) {
//            startForegroundServiceScanning()
//        } else {
//            beaconManager.removeAllMonitorNotifiers()
//            beaconManager.removeAllRangeNotifiers()
//            beaconManager.addMonitorNotifier(this)
//            beaconManager.addRangeNotifier(this)
//            loadEnteredRegions()
//        }

        beaconManager.addMonitorNotifier(this)
        beaconManager.addRangeNotifier(this)
    }

    override fun startMonitoring(region: NotificareRegion, beacons: List<NotificareBeacon>) {
        // Start monitoring the main region.
        val mainBeacon = NotificareBeacon(region.id, region.name, requireNotNull(region.major), null, false, null)
        startMonitoring(mainBeacon)

        // Start monitoring every beacon.
        beacons.forEach { startMonitoring(it) }

        NotificareLogger.debug("Started monitoring ${beacons.size} individual beacons in region '${region.name}'.")
    }

    private fun startMonitoring(beacon: NotificareBeacon) {
        val minor = beacon.minor
        val region = if (minor == null) {
            Region(
                beacon.id,
                Identifier.parse(proximityUUID),
                Identifier.fromInt(beacon.major),
                null
            )
        } else {
            Region(
                beacon.id,
                Identifier.parse(proximityUUID),
                Identifier.fromInt(beacon.major),
                Identifier.fromInt(minor)
            )
        }

        beaconManager.startMonitoring(region)
    }

    override fun stopMonitoring(region: NotificareRegion) {
        // Stop monitoring the main region.
        beaconManager.monitoredRegions
            .filter { it.uniqueId == region.id }
            .forEach { beaconManager.stopMonitoring(it) }

        // Stop monitoring the individual beacons.
        val beacons = beaconManager.monitoredRegions
            .filter { it.id2.toInt() == region.major }
            .onEach { beaconManager.stopMonitoring(it) }

        if (beacons.isNotEmpty()) {
            NotificareLogger.debug("Stopped monitoring ${beacons.size} individual beacons in region '${region.name}'.")
        }
    }

    // region MonitorNotifier

    override fun didEnterRegion(region: Region) {
        NotificareLogger.debug("Entered beacon region ${region.id1} / ${region.id2} / ${region.id3}")

        if (region.id3 == null) {
            // This is the main region. There's no minor.
            beaconManager.startRangingBeacons(region)
        }

        Notificare.geoInternal().handleBeaconEnter(region.uniqueId, region.id2.toInt(), region.id3?.toInt())
    }

    override fun didExitRegion(region: Region) {
        NotificareLogger.debug("Exited beacon region ${region.id1} / ${region.id2} / ${region.id3}")

        if (region.id3 == null) {
            // This is the main region. There's no minor.
            beaconManager.stopRangingBeacons(region)
        }

        Notificare.geoInternal().handleBeaconExit(region.uniqueId, region.id2.toInt(), region.id3.toInt())
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {
        NotificareLogger.debug("State $state for region ${region.id1} / ${region.id2} / ${region.id3}")
    }

    // endregion

    // region RangeNotifier

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<org.altbeacon.beacon.Beacon>?, region: Region?) {
        if (beacons == null || region == null) return

        Notificare.geoInternal().handleRangingBeacons(
            regionId = region.uniqueId,
            beacons = beacons.map { b ->
                Beacon(
                    major = b.id2.toInt(),
                    minor = b.id3.toInt(),
                    proximity = b.distance,
                )
            }
        )
    }

    // endregion
}
