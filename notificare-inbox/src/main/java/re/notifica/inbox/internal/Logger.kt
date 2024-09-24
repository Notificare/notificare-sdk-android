package re.notifica.inbox.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificareInbox",
).apply {
    labelClassIgnoreList = listOf(
        NotificareInboxImpl::class,
    )
}
