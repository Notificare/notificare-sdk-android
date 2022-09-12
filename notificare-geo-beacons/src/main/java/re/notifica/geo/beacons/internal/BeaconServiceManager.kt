package re.notifica.geo.beacons.internal

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.RangedBeacon
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.geo.beacons.*
import re.notifica.geo.beacons.ktx.geoInternal
import re.notifica.geo.internal.BeaconServiceManager
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_NOTIFICATION_OPENED
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareRegion
import re.notifica.internal.NotificareLogger
import java.util.concurrent.atomic.AtomicInteger

private const val BEACON_LAYOUT_APPLE = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"

// AltBeacon.uniqueId   = NotificareRegion.id / NotificareBeacon.id
// AltBeacon.id1        = Proximity UUID
// AltBeacon.id2        = Major
// AltBeacon.id3        = Minor

@Keep
@InternalNotificareApi
public class BeaconServiceManager(
    proximityUUID: String,
) : BeaconServiceManager(proximityUUID), MonitorNotifier, RangeNotifier {

    private val beaconManager: BeaconManager
    private val notificationSequence = AtomicInteger()

    init {
        val context = Notificare.requireContext()
        val options = checkNotNull(Notificare.options)

        beaconManager = BeaconManager.getInstanceForApplication(context)
        beaconManager.beaconParsers.clear()
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BEACON_LAYOUT_APPLE))
        beaconManager.foregroundBetweenScanPeriod = options.beaconForegroundScanInterval
        beaconManager.backgroundBetweenScanPeriod = options.beaconBackgroundScanInterval

        RangedBeacon.setSampleExpirationMilliseconds(options.beaconSampleExpiration)

        if (options.beaconForegroundServiceEnabled) {
            enableForegroundService()
        }

        beaconManager.addMonitorNotifier(this)
        beaconManager.addRangeNotifier(this)
    }

    override fun startMonitoring(region: NotificareRegion, beacons: List<NotificareBeacon>) {
        // Start monitoring the main region.
        val mainBeacon = NotificareBeacon(region.id, region.name, requireNotNull(region.major), null)
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
            .forEach {
                beaconManager.stopRangingBeacons(it)
                beaconManager.stopMonitoring(it)
            }

        // Stop monitoring the individual beacons.
        val beacons = beaconManager.monitoredRegions
            .filter { it.id2.toInt() == region.major }
            .onEach { beaconManager.stopMonitoring(it) }

        if (beacons.isNotEmpty()) {
            NotificareLogger.debug("Stopped monitoring ${beacons.size} individual beacons in region '${region.name}'.")
        }
    }

    override fun clearMonitoring() {
        beaconManager.monitoredRegions.forEach {
            beaconManager.stopRangingBeacons(it)
            beaconManager.stopMonitoring(it)
        }
    }

    private fun enableForegroundService() {
        val options = checkNotNull(Notificare.options)
        val channel = options.beaconServiceNotificationChannel

        val openIntent = Intent().apply {
            action = Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            setPackage(Notificare.requireContext().packageName)
        }

        val openPendingIntent: PendingIntent? =
            if (openIntent.resolveActivity(Notificare.requireContext().packageManager) != null) {
                PendingIntent.getActivity(
                    Notificare.requireContext(),
                    createUniqueNotificationId(),
                    Intent().apply {
                        action = Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        setPackage(Notificare.requireContext().packageName)
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                null
            }

        val notification = NotificationCompat.Builder(Notificare.requireContext(), channel)
            .setContentIntent(openPendingIntent)
            .setSmallIcon(options.beaconServiceNotificationSmallIcon)
            .setContentTitle(options.beaconServiceNotificationContentTitle)
            .setContentText(options.beaconServiceNotificationContentText)
            .apply {
                if (options.beaconServiceNotificationProgress) {
                    setProgress(100, 0, true)
                }
            }
            .build()

        beaconManager.enableForegroundServiceScanning(notification, 456)
    }

    private fun createUniqueNotificationId(): Int {
        return notificationSequence.incrementAndGet()
    }

    // region MonitorNotifier

    override fun didEnterRegion(region: Region) {
        NotificareLogger.debug("Entered beacon region ${region.id1} / ${region.id2} / ${region.id3}")
        Notificare.geoInternal().handleBeaconEnter(region.uniqueId, region.id2.toInt(), region.id3?.toInt())

//        if (region.id3 == null) {
//            // This is the main region. There's no minor.
//            beaconManager.startRangingBeacons(region)
//        }
    }

    override fun didExitRegion(region: Region) {
        NotificareLogger.debug("Exited beacon region ${region.id1} / ${region.id2} / ${region.id3}")
        Notificare.geoInternal().handleBeaconExit(region.uniqueId, region.id2.toInt(), region.id3?.toInt())

//        if (region.id3 == null) {
//            // This is the main region. There's no minor.
//            beaconManager.stopRangingBeacons(region)
//        }
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {
        NotificareLogger.debug("State $state for region ${region.id1} / ${region.id2} / ${region.id3}")

        if (region.id3 == null) {
            // This is the main region. There's no minor.
            when (state) {
                MonitorNotifier.INSIDE -> beaconManager.startRangingBeacons(region)
                MonitorNotifier.OUTSIDE -> beaconManager.stopRangingBeacons(region)
            }
        }
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
