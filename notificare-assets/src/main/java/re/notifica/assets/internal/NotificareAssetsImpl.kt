package re.notifica.assets.internal

import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareApplicationUnavailableException
import re.notifica.NotificareCallback
import re.notifica.NotificareNotReadyException
import re.notifica.NotificareServiceUnavailableException
import re.notifica.assets.NotificareAssets
import re.notifica.assets.internal.network.push.FetchAssetsResponse
import re.notifica.assets.models.NotificareAsset
import re.notifica.utilities.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.utilities.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.models.NotificareApplication

@Keep
internal object NotificareAssetsImpl : NotificareModule(), NotificareAssets {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "NotificareAssets"
    )

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
        toCallbackFunction(NotificareAssetsImpl::fetch)(group, callback::onSuccess, callback::onFailure)

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            logger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        val application = Notificare.application ?: run {
            logger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.STORAGE] != true) {
            logger.warning("Notificare storage functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.STORAGE)
        }
    }
}
