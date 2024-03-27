package re.notifica.geo.internal

import android.location.Location
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareBeaconSession
import re.notifica.geo.models.NotificareRegion

private const val EARTH_RADIUS: Double = 6371000.0

internal val Location.isValid: Boolean
    get() {
        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0
    }

internal fun NotificareRegion.contains(location: Location): Boolean {
    if (this.isPolygon) {
        return checkNotNull(this.advancedGeometry).contains(location)
    }

    val radLat = Math.toRadians(location.latitude)
    val radLon = Math.toRadians(location.longitude)

    val regionRadLat = Math.toRadians(this.geometry.coordinate.latitude)
    val regionRadLon = Math.toRadians(this.geometry.coordinate.longitude)

    val distance: Double = acos(
        sin(regionRadLat) * sin(radLat) + cos(regionRadLat) * cos(radLat) * cos(regionRadLon - radLon)
    ) * EARTH_RADIUS

    return distance < this.distance
}

private fun NotificareRegion.AdvancedGeometry.contains(location: Location): Boolean {
    var lastPoint = this.coordinates[this.coordinates.size - 1]
    var isInside = false

    for (point in this.coordinates) {
        var x1: Double = lastPoint.longitude
        var x2: Double = point.longitude
        var dx = x2 - x1

        if (abs(dx) > 180.0) {
            // we have, most likely, just jumped the dateline (could do further validation to this effect if needed).  normalise the numbers.
            if (location.longitude > 0) {
                while (x1 < 0) x1 += 360.0
                while (x2 < 0) x2 += 360.0
            } else {
                while (x1 > 0) x1 -= 360.0
                while (x2 > 0) x2 -= 360.0
            }

            dx = x2 - x1
        }

        if (x1 <= location.longitude && x2 > location.longitude || x1 >= location.longitude && x2 < location.longitude) {
            val grad: Double = (point.latitude - lastPoint.latitude) / dx
            val intersectAtLat: Double = lastPoint.latitude + (location.longitude - x1) * grad
            if (intersectAtLat > location.latitude) isInside = !isInside
        }

        lastPoint = point
    }

    return isInside
}

internal fun NotificareBeaconSession.canInsertBeacon(beacon: NotificareBeacon): Boolean {
    val lastEntry = beacons.lastOrNull { it.major == beacon.major && it.minor == beacon.minor }
        ?: return true

    if (lastEntry.proximity != beacon.proximity.ordinal) {
        return true
    }

    val fifteenMinutesAgo = Calendar.getInstance().apply { add(Calendar.MINUTE, -15) }.time
    if (lastEntry.timestamp < fifteenMinutesAgo) {
        return true
    }

    return false
}
