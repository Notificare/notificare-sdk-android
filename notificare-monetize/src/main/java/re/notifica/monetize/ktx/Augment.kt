package re.notifica.monetize.ktx

import re.notifica.Notificare
import re.notifica.monetize.NotificareMonetize
import re.notifica.monetize.internal.NotificareMonetizeImpl

@Suppress("unused")
public fun Notificare.monetize(): NotificareMonetize {
    return NotificareMonetizeImpl
}
