package re.notifica.internal

import re.notifica.InternalNotificareApi

@InternalNotificareApi
public object NotificareUtils {
    internal fun getEnabledPeerModules(): List<String> {
        return NotificareModule.Module.values()
            .filter { it.isPeer && it.isAvailable }
            .map { it.name.lowercase() }
    }
}
