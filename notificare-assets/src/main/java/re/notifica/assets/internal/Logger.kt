package re.notifica.assets.internal

import re.notifica.utilities.logging.NotificareLogger

internal val logger = NotificareLogger(
    tag = "NotificareAssets",
).apply {
    labelClassIgnoreList = listOf(
        NotificareAssetsImpl::class,
    )
}
