package re.notifica.geo.beacons.ktx

import re.notifica.Notificare
import re.notifica.geo.NotificareInternalGeo
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_NOTIFICATION_OPENED
import re.notifica.geo.ktx.geo

internal fun Notificare.geoInternal(): NotificareInternalGeo {
    return geo() as NotificareInternalGeo
}

// region Intent actions

@Deprecated(
    message = "Use the INTENT_ACTION_BEACON_NOTIFICATION_OPENED from the notificare-geo module instead.",
    replaceWith = ReplaceWith(
        expression = "Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED",
        imports = [
            "re.notifica.Notificare",
            "re.notifica.geo.ktx.INTENT_ACTION_BEACON_NOTIFICATION_OPENED"
        ],
    ),
)
public val Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED: String
    get() = Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED

// endregion
