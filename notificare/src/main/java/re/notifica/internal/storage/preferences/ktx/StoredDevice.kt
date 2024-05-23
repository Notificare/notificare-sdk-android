package re.notifica.internal.storage.preferences.ktx

import re.notifica.internal.storage.preferences.entities.StoredDevice
import re.notifica.models.NotificareDevice

internal fun StoredDevice.asPublic(): NotificareDevice {
    return NotificareDevice(
        id = id,
        userId = userId,
        userName = userName,
        timeZoneOffset = timeZoneOffset,
        dnd = dnd,
        userData = userData,
    )
}
