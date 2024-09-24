package re.notifica.scannables.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificareScannables",
).apply {
    labelClassIgnoreList = listOf(
        NotificareScannablesImpl::class,
    )
}
