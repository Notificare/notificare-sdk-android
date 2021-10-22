package re.notifica.geo.ktx

import re.notifica.Notificare
import re.notifica.NotificareEventsModule
import re.notifica.geo.models.NotificareRegionSession
import java.util.*

@Suppress("unused")
public fun NotificareEventsModule.logRegionSession(session: NotificareRegionSession) {
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
