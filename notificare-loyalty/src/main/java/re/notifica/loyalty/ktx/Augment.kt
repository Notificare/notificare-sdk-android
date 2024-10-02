package re.notifica.loyalty.ktx

import re.notifica.Notificare
import re.notifica.loyalty.NotificareLoyalty
import re.notifica.loyalty.internal.NotificareLoyaltyImpl

@Suppress("unused")
public fun Notificare.loyalty(): NotificareLoyalty {
    return NotificareLoyaltyImpl
}

// region Intent actions

public val Notificare.INTENT_ACTION_PASSBOOK_OPENED: String
    get() = "re.notifica.intent.action.PassbookOpened"

// endregion

// region Intent extras

public val Notificare.INTENT_EXTRA_PASSBOOK: String
    get() = "re.notifica.intent.extra.Passbook"

// endregion
