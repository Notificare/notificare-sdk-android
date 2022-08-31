package re.notifica.iam.ktx

import re.notifica.Notificare
import re.notifica.NotificareInternalEventsModule
import re.notifica.iam.NotificareInAppMessaging
import re.notifica.iam.internal.NotificareInAppMessagingImpl
import re.notifica.ktx.events

@Suppress("unused")
public fun Notificare.inAppMessaging(): NotificareInAppMessaging {
    return NotificareInAppMessagingImpl
}

internal fun Notificare.inAppMessagingImplementation(): NotificareInAppMessagingImpl {
    return inAppMessaging() as NotificareInAppMessagingImpl
}

internal fun Notificare.eventsInternal(): NotificareInternalEventsModule {
    return events() as NotificareInternalEventsModule
}

// region Intent extras

@Suppress("unused")
public val Notificare.INTENT_EXTRA_IN_APP_MESSAGE: String
    get() = "re.notifica.intent.extra.InAppMessage"

// endregion
