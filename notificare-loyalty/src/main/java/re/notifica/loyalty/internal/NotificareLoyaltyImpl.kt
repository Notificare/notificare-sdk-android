package re.notifica.loyalty.internal

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareApplicationUnavailableException
import re.notifica.NotificareCallback
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.NotificareNotReadyException
import re.notifica.NotificareServiceUnavailableException
import re.notifica.internal.NotificareModule
import re.notifica.utilities.coroutines.toCallbackFunction
import re.notifica.internal.modules.integrations.NotificareLoyaltyIntegration
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.loyalty.NotificareLoyalty
import re.notifica.loyalty.PassbookActivity
import re.notifica.loyalty.internal.network.push.FetchPassResponse
import re.notifica.loyalty.internal.network.push.FetchPassbookTemplateResponse
import re.notifica.loyalty.internal.network.push.FetchSaveLinksResponse
import re.notifica.loyalty.ktx.INTENT_EXTRA_PASSBOOK
import re.notifica.loyalty.models.NotificarePass
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification

@Keep
internal object NotificareLoyaltyImpl : NotificareModule(), NotificareLoyalty, NotificareLoyaltyIntegration {

    override fun configure() {
        logger.hasDebugLoggingEnabled = checkNotNull(Notificare.options).debugLoggingEnabled
    }

    // region Notificare Loyalty

    override var passbookActivity: Class<out PassbookActivity> = PassbookActivity::class.java

    override suspend fun fetchPassBySerial(serial: String): NotificarePass = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val pass = NotificareRequest.Builder()
            .get("/pass/forserial/$serial")
            .responseDecodable(FetchPassResponse::class)
            .pass

        enhancePass(pass)
    }

    override fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>): Unit =
        toCallbackFunction(::fetchPassBySerial)(serial, callback::onSuccess, callback::onFailure)

    override suspend fun fetchPassByBarcode(barcode: String): NotificarePass = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val pass = NotificareRequest.Builder()
            .get("/pass/forbarcode/$barcode")
            .responseDecodable(FetchPassResponse::class)
            .pass

        enhancePass(pass)
    }

    override fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>): Unit =
        toCallbackFunction(::fetchPassByBarcode)(barcode, callback::onSuccess, callback::onFailure)

    override fun present(activity: Activity, pass: NotificarePass) {
        present(activity, pass, null)
    }

    // endregion

    // region Notificare Loyalty Integration

    override fun handlePassPresentation(
        activity: Activity,
        notification: NotificareNotification,
        callback: NotificareCallback<Unit>,
    ) {
        val serial = extractPassSerial(notification) ?: run {
            logger.warning("Unable to extract the pass' serial from the notification.")

            val error = IllegalArgumentException("Unable to extract the pass' serial from the notification.")
            callback.onFailure(error)

            return
        }

        fetchPassBySerial(
            serial,
            object : NotificareCallback<NotificarePass> {
                override fun onSuccess(result: NotificarePass) {
                    present(activity, result, callback)
                }

                override fun onFailure(e: Exception) {
                    logger.error("Failed to fetch the pass with serial '$serial'.", e)
                    callback.onFailure(e)
                }
            }
        )
    }

    // endregion

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            logger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        if (Notificare.device().currentDevice == null) {
            logger.warning("Notificare device is not yet available.")
            throw NotificareDeviceUnavailableException()
        }

        val application = Notificare.application ?: run {
            logger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.PASSBOOK] != true) {
            logger.warning("Notificare passes functionality is not enabled.")
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

    private suspend fun enhancePass(pass: FetchPassResponse.Pass): NotificarePass = withContext(Dispatchers.IO) {
        val passType = when (pass.version) {
            1 -> pass.passbook?.let { fetchPassType(it) }
            else -> null
        }

        val googlePaySaveLink = when (pass.version) {
            2 -> fetchGooglePaySaveLink(pass.serial)
            else -> null
        }

        NotificarePass(
            id = pass._id,
            type = passType,
            version = pass.version,
            passbook = pass.passbook,
            template = pass.template,
            serial = pass.serial,
            barcode = pass.barcode,
            redeem = pass.redeem,
            redeemHistory = pass.redeemHistory,
            limit = pass.limit,
            token = pass.token,
            data = pass.data ?: emptyMap(),
            date = pass.date,
            googlePaySaveLink = googlePaySaveLink,
        )
    }

    private suspend fun fetchPassType(passbook: String): NotificarePass.PassType = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/passbook/$passbook")
            .responseDecodable(FetchPassbookTemplateResponse::class)
            .passbook
            .passStyle
    }

    private suspend fun fetchGooglePaySaveLink(serial: String): String? = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/pass/savelinks/$serial")
            .responseDecodable(FetchSaveLinksResponse::class)
            .saveLinks
            ?.googlePay
    }

    private fun present(activity: Activity, pass: NotificarePass, callback: NotificareCallback<Unit>?) {
        when (pass.version) {
            1 -> {
                activity.startActivity(
                    Intent(activity, passbookActivity)
                        .putExtra(Notificare.INTENT_EXTRA_PASSBOOK, pass)
                )

                callback?.onSuccess(Unit)
            }
            2 -> {
                val url = pass.googlePaySaveLink ?: run {
                    logger.warning("Cannot present the pass without a Google Pay link.")

                    val error = IllegalArgumentException("Cannot present the pass without a Google Pay link.")
                    callback?.onFailure(error)

                    return
                }

                try {
                    val intent = Intent().setAction(Intent.ACTION_VIEW)
                        .setData(Uri.parse(url))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    activity.startActivity(intent)
                    callback?.onSuccess(Unit)
                } catch (e: ActivityNotFoundException) {
                    logger.error("Failed to present the pass.", e)
                    callback?.onFailure(e)
                }
            }
            else -> {
                logger.error("Unsupported pass version: ${pass.version}")

                val error = IllegalArgumentException("Unsupported pass version: ${pass.version}")
                callback?.onFailure(error)
            }
        }
    }
}
