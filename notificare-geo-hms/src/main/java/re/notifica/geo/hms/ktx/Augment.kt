package re.notifica.geo.hms.ktx

import re.notifica.Notificare
import re.notifica.geo.NotificareInternalGeo
import re.notifica.geo.ktx.geo

internal fun Notificare.geoInternal(): NotificareInternalGeo {
    return geo() as NotificareInternalGeo
}
