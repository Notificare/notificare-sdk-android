package re.notifica.monetize.gms.ktx

import re.notifica.Notificare
import re.notifica.monetize.NotificareInternalMonetize
import re.notifica.monetize.ktx.monetize

internal fun Notificare.monetizeInternal(): NotificareInternalMonetize {
    return monetize() as NotificareInternalMonetize
}
