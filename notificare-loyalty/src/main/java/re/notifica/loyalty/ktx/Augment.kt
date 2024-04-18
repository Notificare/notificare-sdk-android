package re.notifica.loyalty.ktx

import re.notifica.Notificare
import re.notifica.internal.NotificareModule
import re.notifica.internal.modules.integrations.NotificareGeoIntegration
import re.notifica.loyalty.NotificareLoyalty
import re.notifica.loyalty.internal.NotificareLoyaltyImpl

@Suppress("unused")
public fun Notificare.loyalty(): NotificareLoyalty {
    return NotificareLoyaltyImpl
}

internal fun Notificare.loyaltyImplementation(): NotificareLoyaltyImpl {
    return loyalty() as NotificareLoyaltyImpl
}

@Suppress("unused")
internal fun Notificare.geoIntegration(): NotificareGeoIntegration? {
    return NotificareModule.Module.GEO.instance as? NotificareGeoIntegration
}

// region Intent actions

public val Notificare.INTENT_ACTION_PASSBOOK_OPENED: String
    get() = "re.notifica.intent.action.PassbookOpened"

public val Notificare.INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED: String
    get() = "re.notifica.intent.action.RelevanceNotificationDeleted"

// endregion

// region Intent extras

public val Notificare.INTENT_EXTRA_PASSBOOK: String
    get() = "re.notifica.intent.extra.Passbook"

// endregion
