package re.notifica.assets.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareException
import re.notifica.assets.NotificareAssets
import re.notifica.assets.internal.network.push.FetchAssetsResponse
import re.notifica.assets.models.NotificareAsset
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device

internal object NotificareAssetsImpl : NotificareModule(), NotificareAssets {

    override suspend fun fetchAssets(group: String): List<NotificareAsset> = withContext(Dispatchers.IO) {
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
            .query("deviceID", Notificare.device().currentDevice?.id)
            .query("userID", Notificare.device().currentDevice?.userId)
            .responseDecodable(FetchAssetsResponse::class)
            .assets
            .map { it.toModel() }
    }

    override fun fetchAssets(group: String, callback: NotificareCallback<List<NotificareAsset>>): Unit =
        toCallbackFunction(NotificareAssetsImpl::fetchAssets)(group, callback)

}
