package re.notifica.assets.ktx

import re.notifica.Notificare
import re.notifica.assets.NotificareAssets
import re.notifica.assets.internal.NotificareAssetsImpl

@Suppress("unused")
public fun Notificare.assets(): NotificareAssets {
    return NotificareAssetsImpl
}
