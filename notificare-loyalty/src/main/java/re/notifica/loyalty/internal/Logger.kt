package re.notifica.loyalty.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificareLoyalty",
).apply {
    labelClassIgnoreList = listOf(
        NotificareLoyaltyImpl::class,
    )
}
