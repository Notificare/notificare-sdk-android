package re.notifica.loyalty.ktx

import re.notifica.Notificare
import re.notifica.loyalty.NotificareLoyalty
import re.notifica.loyalty.internal.NotificareLoyaltyImpl

@Suppress("unused")
public fun Notificare.loyalty(): NotificareLoyalty {
    return NotificareLoyaltyImpl
}
