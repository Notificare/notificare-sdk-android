package re.notifica.assets

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareException
import re.notifica.assets.internal.network.push.FetchAssetsResponse
import re.notifica.assets.models.NotificareAsset
import re.notifica.internal.NotificareLogger
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.modules.NotificareModule

public object NotificareAssets : NotificareModule() {

    // region Notificare Module

    override fun configure() {}

    override suspend fun launch() {}

    override suspend fun unlaunch() {}

    // endregion

    public suspend fun fetchAssets(group: String): List<NotificareAsset> = withContext(Dispatchers.IO) {
        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareException.NotReady()
        }

        if (application.services["storage"] != true) {
            NotificareLogger.warning("Notificare storage functionality is not enabled.")
            throw NotificareException.NotReady()
        }

        NotificareRequest.Builder()
            .get("/asset/forgroup/$group")
            .query("deviceID", Notificare.deviceManager.currentDevice?.id)
            .query("userID", Notificare.deviceManager.currentDevice?.userId)
            .responseDecodable(FetchAssetsResponse::class)
            .assets
            .map { it.toModel() }
    }

    public fun fetchAssets(group: String, callback: NotificareCallback<List<NotificareAsset>>): Unit =
        toCallbackFunction(::fetchAssets)(group, callback)
}
