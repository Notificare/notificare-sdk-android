package re.notifica.assets

import re.notifica.NotificareCallback
import re.notifica.assets.models.NotificareAsset

public interface NotificareAssets {

    /**
     * Fetches a list of [NotificareAsset] for a specified group.
     *
     * @param group The name of the group whose assets are to be fetched.
     * @return A list of [NotificareAsset] belonging to the specified group.
     *
     * @see NotificareAsset for the model class representing an asset.
     */
    public suspend fun fetch(group: String): List<NotificareAsset>

    /**
     * Fetches a list of [NotificareAsset] for the specified group and returns the result via a callback.
     *
     * @param group The name of the group whose assets are to be fetched.
     * @param callback A callback that will be invoked with the result of the fetch operation.
     *                 This will provide either the list of [NotificareAsset] on success
     *                 or an error on failure.
     *
     * @see NotificareAsset for the model class representing an asset.
     */
    public fun fetch(group: String, callback: NotificareCallback<List<NotificareAsset>>)
}
