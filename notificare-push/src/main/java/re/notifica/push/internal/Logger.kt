package re.notifica.push.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificarePush",
).apply {
    labelClassIgnoreList = listOf(
        NotificarePushImpl::class,
    )
}
