package re.notifica.inbox.user.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificareUserInbox",
).apply {
    labelClassIgnoreList = listOf(
        NotificareUserInboxImpl::class,
    )
}
