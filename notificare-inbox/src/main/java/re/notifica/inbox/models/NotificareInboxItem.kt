package re.notifica.inbox.models

import re.notifica.models.NotificareNotification
import java.util.*

data class NotificareInboxItem internal constructor(
    val id: String,
    internal var _notification: NotificareNotification,
    val time: Date,
    internal var _opened: Boolean,
    internal val visible: Boolean,
    val expires: Date?,
) {

    val notification: NotificareNotification
        get() = _notification

    val opened: Boolean
        get() = _opened

    internal val expired: Boolean
        get() {
            val expiresAt = expires ?: return false
            return expiresAt.before(Date())
        }
}
