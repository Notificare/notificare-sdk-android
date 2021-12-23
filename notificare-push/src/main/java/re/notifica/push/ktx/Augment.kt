package re.notifica.push.ktx

import re.notifica.Notificare
import re.notifica.NotificareInternalDeviceModule
import re.notifica.NotificareInternalEventsModule
import re.notifica.internal.NotificareModule
import re.notifica.internal.modules.integrations.NotificareLoyaltyIntegration
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.push.NotificarePush
import re.notifica.push.internal.NotificarePushImpl

@Suppress("unused")
public fun Notificare.push(): NotificarePush {
    return NotificarePushImpl
}

internal fun Notificare.deviceInternal(): NotificareInternalDeviceModule {
    return device() as NotificareInternalDeviceModule
}

internal fun Notificare.eventsInternal(): NotificareInternalEventsModule {
    return events() as NotificareInternalEventsModule
}

internal fun Notificare.loyaltyIntegration(): NotificareLoyaltyIntegration? {
    return NotificareModule.Module.LOYALTY.instance as? NotificareLoyaltyIntegration
}

// region Intent actions

public val Notificare.INTENT_ACTION_REMOTE_MESSAGE_OPENED: String
    get() = "re.notifica.intent.action.RemoteMessageOpened"

public val Notificare.INTENT_ACTION_NOTIFICATION_RECEIVED: String
    get() = "re.notifica.intent.action.NotificationReceived"

public val Notificare.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED: String
    get() = "re.notifica.intent.action.SystemNotificationReceived"

public val Notificare.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED: String
    get() = "re.notifica.intent.action.UnknownNotificationReceived"

public val Notificare.INTENT_ACTION_NOTIFICATION_OPENED: String
    get() = "re.notifica.intent.action.NotificationOpened"

public val Notificare.INTENT_ACTION_ACTION_OPENED: String
    get() = "re.notifica.intent.action.ActionOpened"

public val Notificare.INTENT_ACTION_QUICK_RESPONSE: String
    get() = "re.notifica.intent.action.NotificationQuickResponse"

// endregion

// region Intent extras

public val Notificare.INTENT_EXTRA_REMOTE_MESSAGE: String
    get() = "re.notifica.intent.extra.RemoteMessage"

public val Notificare.INTENT_EXTRA_TEXT_RESPONSE: String
    get() = "re.notifica.intent.extra.TextResponse"

// endregion
