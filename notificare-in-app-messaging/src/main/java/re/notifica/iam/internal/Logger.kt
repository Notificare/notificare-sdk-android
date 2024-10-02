package re.notifica.iam.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificareInAppMessaging",
).apply {
    labelClassIgnoreList = listOf(
        NotificareInAppMessagingImpl::class,
    )
}
