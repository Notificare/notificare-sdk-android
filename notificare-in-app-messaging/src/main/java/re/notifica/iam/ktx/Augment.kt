package re.notifica.iam.ktx

import re.notifica.Notificare
import re.notifica.iam.NotificareInAppMessaging
import re.notifica.iam.internal.NotificareInAppMessagingImpl

@Suppress("unused")
public fun Notificare.inAppMessaging(): NotificareInAppMessaging {
    return NotificareInAppMessagingImpl
}
