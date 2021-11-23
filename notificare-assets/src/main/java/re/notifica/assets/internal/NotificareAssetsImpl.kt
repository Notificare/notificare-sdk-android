package re.notifica.assets.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.*
import re.notifica.assets.NotificareAssets
import re.notifica.assets.internal.network.push.FetchAssetsResponse
import re.notifica.assets.models.NotificareAsset
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.models.NotificareApplication

internal object NotificareAssetsImpl : NotificareModule(), NotificareAssets {

    override suspend fun fetch(group: String): List<NotificareAsset> = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .get("/asset/forgroup/$group")
            .query("deviceID", Notificare.device().currentDevice?.id)
            .query("userID", Notificare.device().currentDevice?.userId)
            .responseDecodable(FetchAssetsResponse::class)
            .assets
            .map { it.toModel() }
    }

    override fun fetch(group: String, callback: NotificareCallback<List<NotificareAsset>>): Unit =
        toCallbackFunction(NotificareAssetsImpl::fetch)(group, callback)

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.STORAGE] != true) {
            NotificareLogger.warning("Notificare storage functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.STORAGE)
        }
    }
}
