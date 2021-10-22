package re.notifica.push.ui.ktx

import re.notifica.Notificare
import re.notifica.push.ui.NotificareInternalPushUI
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.internal.NotificarePushUIImpl

@Suppress("unused")
public fun Notificare.pushUI(): NotificarePushUI {
    return NotificarePushUIImpl
}

internal fun Notificare.pushUIInternal(): NotificareInternalPushUI {
    return pushUI() as NotificareInternalPushUI
}

internal fun Notificare.pushUIImplementation(): NotificarePushUIImpl {
    return pushUI() as NotificarePushUIImpl
}
