package re.notifica.push.ui.gms.ktx

import re.notifica.Notificare
import re.notifica.push.ui.NotificareInternalPushUI
import re.notifica.push.ui.ktx.pushUI

internal fun Notificare.pushUIInternal(): NotificareInternalPushUI {
    return pushUI() as NotificareInternalPushUI
}
