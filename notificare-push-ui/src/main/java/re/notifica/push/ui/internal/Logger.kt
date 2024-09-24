package re.notifica.push.ui.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificarePushUI",
).apply {
    labelClassIgnoreList = listOf(
        NotificarePushUIImpl::class,
    )
}
