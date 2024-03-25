package re.notifica.geo.beacons

import android.os.Build
import re.notifica.Notificare
import re.notifica.internal.NotificareOptions

private const val DEFAULT_FOREGROUND_SCAN_INTERVAL: Long = 0L // always-on
private const val DEFAULT_BACKGROUND_SCAN_INTERVAL: Long = 30000L // 30 seconds
private const val DEFAULT_BACKGROUND_SCAN_INTERVAL_O: Long = 900000L // 15 minutes
private const val DEFAULT_SAMPLE_EXPIRATION: Long = 10000L // 10 seconds
private const val DEFAULT_NOTIFICATION_CHANNEL: String = "notificare_channel_default"

public val NotificareOptions.beaconForegroundScanInterval: Long
    get() {
        return metadata.getLong(
            "re.notifica.geo.beacons.foreground_scan_interval",
            DEFAULT_FOREGROUND_SCAN_INTERVAL
        )
    }

public val NotificareOptions.beaconBackgroundScanInterval: Long
    get() {
        val defaultInterval = when {
            beaconForegroundServiceEnabled -> DEFAULT_FOREGROUND_SCAN_INTERVAL
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> DEFAULT_BACKGROUND_SCAN_INTERVAL_O
            else -> DEFAULT_BACKGROUND_SCAN_INTERVAL
        }

        return metadata.getLong("re.notifica.geo.beacons.background_scan_interval", defaultInterval)
    }

public val NotificareOptions.beaconSampleExpiration: Long
    get() {
        return metadata.getLong(
            "re.notifica.geo.beacons.sample_expiration",
            DEFAULT_SAMPLE_EXPIRATION
        )
    }

public val NotificareOptions.beaconForegroundServiceEnabled: Boolean
    get() {
        return metadata.getBoolean("re.notifica.geo.beacons.foreground_service_enabled", false)
    }

public val NotificareOptions.beaconServiceNotificationChannel: String
    get() {
        return metadata.getString(
            "re.notifica.geo.beacons.service_notification_channel",
            DEFAULT_NOTIFICATION_CHANNEL
        )
    }

public val NotificareOptions.beaconServiceNotificationSmallIcon: Int
    get() {
        return metadata.getInt("re.notifica.geo.beacons.service_notification_small_icon", info.icon)
    }

public val NotificareOptions.beaconServiceNotificationContentTitle: String?
    get() {
        return metadata.getString(
            "re.notifica.geo.beacons.service_notification_content_title",
            null
        )
    }

public val NotificareOptions.beaconServiceNotificationContentText: String
    get() {
        val defaultText = Notificare.requireContext().getString(
            R.string.notificare_beacons_notification_content_text
        )

        return metadata.getString(
            "re.notifica.geo.beacons.service_notification_content_text",
            defaultText
        )
    }

public val NotificareOptions.beaconServiceNotificationProgress: Boolean
    get() {
        return metadata.getBoolean("re.notifica.geo.beacons.service_notification_progress", false)
    }
