package re.notifica.assets

import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.assets.ktx.assets
import re.notifica.assets.models.NotificareAsset

public object NotificareAssetsCompat {

    @JvmStatic
    public fun fetch(group: String, callback: NotificareCallback<List<NotificareAsset>>) {
        Notificare.assets().fetch(group, callback)
    }
}
