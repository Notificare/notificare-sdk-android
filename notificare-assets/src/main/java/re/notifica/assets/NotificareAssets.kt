package re.notifica.assets

import re.notifica.NotificareCallback
import re.notifica.assets.models.NotificareAsset

public interface NotificareAssets {

    public suspend fun fetch(group: String): List<NotificareAsset>

    public fun fetch(group: String, callback: NotificareCallback<List<NotificareAsset>>)
}
