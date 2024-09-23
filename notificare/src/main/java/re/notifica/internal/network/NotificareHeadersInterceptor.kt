package re.notifica.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import re.notifica.Notificare
import re.notifica.internal.ktx.unsafeHeader
import re.notifica.ktx.device
import re.notifica.utilities.content.applicationVersion
import re.notifica.utilities.device.deviceLanguage
import re.notifica.utilities.device.deviceRegion
import re.notifica.utilities.networking.userAgent

internal class NotificareHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val context = Notificare.requireContext()

        val request = chain.request().newBuilder()
            .header(
                "Accept-Language",
                Notificare.device().preferredLanguage
                    ?: "$deviceLanguage-$deviceRegion"
            )
            .unsafeHeader("User-Agent", context.userAgent(Notificare.SDK_VERSION))
            .header("X-Notificare-SDK-Version", Notificare.SDK_VERSION)
            .header("X-Notificare-App-Version", context.applicationVersion)
            .build()

        return chain.proceed(request)
    }
}
