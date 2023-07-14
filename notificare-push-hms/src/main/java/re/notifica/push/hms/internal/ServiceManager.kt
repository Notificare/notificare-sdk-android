package re.notifica.push.hms.internal

import androidx.annotation.Keep
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging
import kotlinx.coroutines.launch
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.ktx.coroutineScope
import re.notifica.models.NotificareTransport
import re.notifica.push.hms.ktx.pushInternal
import re.notifica.push.internal.ServiceManager

@Keep
@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override val transport: NotificareTransport
        get() = NotificareTransport.HMS

    override fun requestPushToken() {
        Thread {
            try {
                val context = Notificare.requireContext()

                // read from agconnect-services.json
                val options = AGConnectOptionsBuilder().build(context)
                val appId = options.getString("client/app_id")
                val token = HmsInstanceId.getInstance(context).getToken(appId, HmsMessaging.DEFAULT_TOKEN_SCOPE)

                if (token != null && token.isNotEmpty()) {
                    Notificare.coroutineScope.launch {
                        try {
                            Notificare.pushInternal().registerPushToken(transport, token = token)
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
}
