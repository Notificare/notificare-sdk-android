package re.notifica.push.ktx

import re.notifica.Notificare
import re.notifica.NotificareInternalEventsModule
import re.notifica.ktx.events
import re.notifica.push.NotificareInternalPush
import re.notifica.push.NotificarePush
import re.notifica.push.internal.NotificarePushImpl

@Suppress("unused")
public fun Notificare.push(): NotificarePush {
    return NotificarePushImpl
}

internal fun Notificare.pushInternal(): NotificareInternalPush {
    return push() as NotificareInternalPush
}

internal fun Notificare.eventsInternal(): NotificareInternalEventsModule {
    return events() as NotificareInternalEventsModule
}

// region Intent actions

public val Notificare.INTENT_ACTION_SUBSCRIPTION_CHANGED: String
    get() = "re.notifica.intent.action.SubscriptionChanged"

public val Notificare.INTENT_ACTION_TOKEN_CHANGED: String
    get() = "re.notifica.intent.action.TokenChanged"

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

public val Notificare.INTENT_ACTION_LIVE_ACTIVITY_UPDATE: String
    get() = "re.notifica.intent.action.LiveActivityUpdate"

// endregion

// region Intent extras

public val Notificare.INTENT_EXTRA_SUBSCRIPTION: String
    get() = "re.notifica.intent.extra.Subscription"

public val Notificare.INTENT_EXTRA_TOKEN: String
    get() = "re.notifica.intent.extra.Token"

public val Notificare.INTENT_EXTRA_REMOTE_MESSAGE: String
    get() = "re.notifica.intent.extra.RemoteMessage"

public val Notificare.INTENT_EXTRA_TEXT_RESPONSE: String
    get() = "re.notifica.intent.extra.TextResponse"

public val Notificare.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE: String
    get() = "re.notifica.intent.extra.LiveActivityUpdate"

public val Notificare.INTENT_EXTRA_DELIVERY_MECHANISM: String
    get() = "re.notifica.intent.extra.DeliveryMechanism"

// endregion
