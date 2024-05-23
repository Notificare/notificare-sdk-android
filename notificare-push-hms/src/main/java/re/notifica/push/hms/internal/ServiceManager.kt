package re.notifica.push.hms.internal

import androidx.annotation.Keep
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.models.NotificareTransport
import re.notifica.push.internal.ServiceManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Keep
@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override val transport: NotificareTransport
        get() = NotificareTransport.HMS

    override suspend fun getPushToken(): String = suspendCancellableCoroutine { continuation ->
        Thread {
            try {
                val context = Notificare.requireContext()

                // read from agconnect-services.json
                val options = AGConnectOptionsBuilder().build(context)
                val appId = options.getString("client/app_id")
                val token = HmsInstanceId.getInstance(context).getToken(appId, HmsMessaging.DEFAULT_TOKEN_SCOPE)

                if (token != null && token.isNotEmpty()) {
                    continuation.resume(token)
                } else {
                    continuation.resumeWithException(Exception("Failed to retrieve HMS token."))
                }
            } catch (e: ApiException) {
                continuation.resumeWithException(e)
            }
        }.start()
    }
}
