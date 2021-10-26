package re.notifica.inbox.ktx

import re.notifica.Notificare
import re.notifica.inbox.NotificareInbox
import re.notifica.inbox.internal.NotificareInboxImpl

@Suppress("unused")
public fun Notificare.inbox(): NotificareInbox {
    return NotificareInboxImpl
}

internal fun Notificare.inboxImplementation(): NotificareInboxImpl {
    return inbox() as NotificareInboxImpl
}
