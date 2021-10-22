package re.notifica.assets

import re.notifica.NotificareCallback
import re.notifica.assets.models.NotificareAsset

public interface NotificareAssets {

    public suspend fun fetchAssets(group: String): List<NotificareAsset>

    public fun fetchAssets(group: String, callback: NotificareCallback<List<NotificareAsset>>)
}
