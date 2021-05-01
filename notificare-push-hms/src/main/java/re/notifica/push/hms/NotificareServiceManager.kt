package re.notifica.push.hms

import android.content.Context
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging
import com.huawei.hms.push.RemoteMessage
import kotlinx.coroutines.runBlocking
import re.notifica.NotificareLogger
import re.notifica.models.NotificareTransport
import re.notifica.push.NotificarePush
import re.notifica.push.NotificareServiceManager

@Suppress("unused")
class NotificareServiceManager(
    private val context: Context,
) : NotificareServiceManager {

    override val transport: NotificareTransport
        get() = NotificareTransport.HMS

    override val hasMobileServicesAvailable: Boolean
        get() = HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) == ConnectionResult.SUCCESS

    init {
        if (!hasMobileServicesAvailable) {
            throw IllegalStateException("Huawei Mobile Services are not available.")
        }
    }

    override fun registerDeviceToken() {
        Thread {
            try {
                // read from agconnect-services.json
                val appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id")
                val token = HmsInstanceId.getInstance(context).getToken(appId, HmsMessaging.DEFAULT_TOKEN_SCOPE)

                if (token != null && token.isNotEmpty()) {
                    runBlocking {
                        try {
                            NotificarePush.registerPushToken(transport, token = token)
                            NotificareLogger.debug("Registered the device with a HMS token.")
                        } catch (e: Exception) {
                            NotificareLogger.debug("Failed to register the device with a HMS token.", e)
                        }
                    }
                } else {
                    NotificareLogger.error("Failed to retrieve HMS token.")
                }
            } catch (e: ApiException) {
                NotificareLogger.error("Failed to retrieve HMS token.", e)
            }
        }.start()
    }

    companion object {
        fun isNotificareNotification(remoteMessage: RemoteMessage): Boolean {
            return remoteMessage.dataOfMap?.get("x-sender") == "notificare"
        }
    }
}
