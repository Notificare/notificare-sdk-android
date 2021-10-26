package re.notifica.geo.ktx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareEventsModule
import re.notifica.geo.models.NotificareBeaconSession
import re.notifica.geo.models.NotificareRegionSession
import re.notifica.internal.ktx.toCallbackFunction
import java.util.*

@Suppress("unused")
internal suspend fun NotificareEventsModule.logRegionSession(
    session: NotificareRegionSession
): Unit = withContext(Dispatchers.IO) {
    val sessionEnd = session.end ?: Date()
    val sessionLength = (sessionEnd.time - session.start.time) / 1000.0

    Notificare.eventsInternal().log(
        event = "re.notifica.event.region.Session",
        data = mapOf(
            "region" to session.regionId,
            "start" to session.start,
            "end" to sessionEnd,
            "length" to sessionLength,
            "locations" to session.locations.map { location ->
                mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "altitude" to location.altitude,
                    "course" to location.course,
                    "speed" to location.speed,
                    "horizontalAccuracy" to location.horizontalAccuracy,
                    "verticalAccuracy" to location.verticalAccuracy,
                    "timestamp" to location.timestamp,
                )
            }
        ),
    )
}

internal fun NotificareEventsModule.logRegionSession(
    session: NotificareRegionSession,
    callback: NotificareCallback<Unit>,
): Unit = toCallbackFunction(::logRegionSession)(session, callback)


@Suppress("unused")
internal suspend fun NotificareEventsModule.logBeaconSession(
    session: NotificareBeaconSession
): Unit = withContext(Dispatchers.IO) {
    val sessionEnd = session.end ?: Date()
    val sessionLength = (sessionEnd.time - session.start.time) / 1000.0

    Notificare.eventsInternal().log(
        event = "re.notifica.event.beacon.Session",
        data = mapOf(
            "fence" to session.regionId,
            "start" to session.start,
            "end" to session.end,
            "length" to sessionLength,
            "beacons" to session.beacons.map { beacon ->
                mapOf(
                    "proximity" to beacon.proximity,
                    "major" to beacon.major,
                    "minor" to beacon.minor,
                    "location" to beacon.location?.let { location ->
                        mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                        )
                    },
                    "timestamp" to beacon.timestamp,
                )
            }
        )
    )
}

internal fun NotificareEventsModule.logBeaconSession(
    session: NotificareBeaconSession,
    callback: NotificareCallback<Unit>,
): Unit = toCallbackFunction(::logBeaconSession)(session, callback)
