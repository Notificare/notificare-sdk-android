package re.notifica.push.ui.ktx

import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.modules.NotificareLoyaltyIntegration
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

internal fun Notificare.loyaltyIntegration(): NotificareLoyaltyIntegration? {
    if (!NotificareModule.Module.LOYALTY.isAvailable) {
        NotificareLogger.debug("Loyalty module is not available.")
        return null
    }

    val instance = NotificareModule.Module.LOYALTY.instance ?: run {
        NotificareLogger.debug("Unable to acquire Loyalty module instance.")
        return null
    }

    val integration = instance as? NotificareLoyaltyIntegration ?: run {
        NotificareLogger.debug("Loyalty module instance does not comply with the NotificareLoyaltyIntegration interface.")
        return null
    }

    return integration
}
