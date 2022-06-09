package re.notifica.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import re.notifica.Notificare
import re.notifica.internal.NotificareUtils
import re.notifica.internal.ktx.unsafeHeader
import re.notifica.ktx.device

internal class NotificareHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(
                "Accept-Language",
                Notificare.device().preferredLanguage
                    ?: "${NotificareUtils.deviceLanguage}-${NotificareUtils.deviceRegion}"
            )
            .unsafeHeader("User-Agent", NotificareUtils.userAgent)
            .header("X-Notificare-SDK-Version", Notificare.SDK_VERSION)
            .header("X-Notificare-App-Version", NotificareUtils.applicationVersion)
            .build()

        return chain.proceed(request)
    }
}
