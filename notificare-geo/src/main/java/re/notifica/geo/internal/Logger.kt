package re.notifica.geo.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificareGeo",
).apply {
    labelClassIgnoreList = listOf(
        NotificareGeoImpl::class,
    )
}
