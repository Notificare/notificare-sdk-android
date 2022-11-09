package re.notifica.inbox.user.ktx

import re.notifica.Notificare
import re.notifica.inbox.user.NotificareUserInbox
import re.notifica.inbox.user.internal.NotificareUserInboxImpl

@Suppress("unused")
public fun Notificare.userInbox(): NotificareUserInbox {
    return NotificareUserInboxImpl
}
