package re.notifica.push.ui.ktx

import re.notifica.Notificare

import re.notifica.internal.NotificareModule
import re.notifica.internal.modules.integrations.NotificareLoyaltyIntegration
import re.notifica.push.ui.NotificareInternalPushUI
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.internal.NotificarePushUIImpl
import re.notifica.push.ui.internal.logger

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
        logger.debug("Loyalty module is not available.")
        return null
    }

    val instance = NotificareModule.Module.LOYALTY.instance ?: run {
        logger.debug("Unable to acquire Loyalty module instance.")
        return null
    }

    val integration = instance as? NotificareLoyaltyIntegration ?: run {
        logger.debug(
            "Loyalty module instance does not comply with the NotificareLoyaltyIntegration interface."
        )
        return null
    }

    return integration
}
