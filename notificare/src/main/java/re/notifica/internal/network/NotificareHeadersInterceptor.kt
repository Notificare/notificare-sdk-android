package re.notifica.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import re.notifica.Notificare
import re.notifica.internal.ktx.unsafeHeader
import re.notifica.ktx.device
import re.notifica.utilities.deviceLanguage
import re.notifica.utilities.deviceRegion
import re.notifica.utilities.getApplicationVersion
import re.notifica.utilities.getUserAgent

internal class NotificareHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(
                "Accept-Language",
                Notificare.device().preferredLanguage
                    ?: "$deviceLanguage-$deviceRegion"
            )
            .unsafeHeader(
                "User-Agent",
                getUserAgent(Notificare.requireContext().applicationContext, Notificare.SDK_VERSION)
            )
            .header("X-Notificare-SDK-Version", Notificare.SDK_VERSION)
            .header("X-Notificare-App-Version", getApplicationVersion(Notificare.requireContext().applicationContext))
            .build()

        return chain.proceed(request)
    }
}
