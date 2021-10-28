package re.notifica.loyalty.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.*
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.modules.NotificareLoyaltyIntegration
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.loyalty.NotificareLoyalty
import re.notifica.loyalty.internal.network.push.FetchPassResponse
import re.notifica.loyalty.internal.network.push.FetchSaveLinksResponse
import re.notifica.loyalty.internal.network.push.toModel
import re.notifica.loyalty.models.NotificarePass
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification

internal object NotificareLoyaltyImpl : NotificareModule(), NotificareLoyalty, NotificareLoyaltyIntegration {

    // region Notificare Loyalty

    override suspend fun fetchPassBySerial(serial: String): NotificarePass = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .get("/pass/forserial/$serial")
            .responseDecodable(FetchPassResponse::class)
            .pass
            .toModel()
    }

    override fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>): Unit =
        toCallbackFunction(::fetchPassBySerial)(serial, callback)

    override suspend fun fetchPassByBarcode(barcode: String): NotificarePass = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .get("/pass/forbarcode/$barcode")
            .responseDecodable(FetchPassResponse::class)
            .pass
            .toModel()


    }

    override fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>): Unit =
        toCallbackFunction(::fetchPassByBarcode)(barcode, callback)

    // endregion

    // region Notificare Loyalty Integration

    override fun handlePresentationDecision(
        notification: NotificareNotification,
        callback: NotificareLoyaltyIntegration.PresentationDecisionCallback
    ) {
        // TODO check wallet
        val includedInWallet = false

        val serial = extractPassSerial(notification) ?: run {
            NotificareLogger.warning("Unable to extract the pass' serial from the notification.")

            val error = IllegalArgumentException("Unable to extract the pass' serial from the notification.")
            callback.onFailure(error)

            return
        }

        GlobalScope.launch {
            try {
                val pass = fetchPassBySerial(serial)
                when (pass.version) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            callback.presentPKPass(includedInWallet)
                        }
                    }
                    2 -> {
                        val url = fetchGooglePaySaveLink(serial)
                            ?: throw IllegalArgumentException("Pass v2 doesn't contain a Google Pay link.")

                        withContext(Dispatchers.Main) {
                            callback.presentGooglePass(url)
                        }
                    }
                    else -> throw IllegalArgumentException("Unsupported pass version: ${pass.version}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    override fun handleStorageUpdate(
        notification: NotificareNotification,
        includeInWallet: Boolean,
        callback: NotificareCallback<Unit>
    ) {

    }

    // endregion

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

        if (application.services[NotificareApplication.ServiceKeys.PASSBOOK] != true) {
            NotificareLogger.warning("Notificare passes functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.PASSBOOK)
        }
    }

    private fun extractPassSerial(notification: NotificareNotification): String? {
        if (notification.type != NotificareNotification.TYPE_PASSBOOK) return null

        val content = notification.content
            .firstOrNull { it.type == NotificareNotification.Content.TYPE_PK_PASS }
            ?: return null

        val url = content.data as? String ?: return null

        val parts = url.split("/")
        if (parts.isEmpty()) return null

        return parts.last()
    }

    private suspend fun fetchGooglePaySaveLink(serial: String): String? = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/pass/savelinks/$serial")
            .responseDecodable(FetchSaveLinksResponse::class)
            .saveLinks
            ?.googlePay
    }
}
