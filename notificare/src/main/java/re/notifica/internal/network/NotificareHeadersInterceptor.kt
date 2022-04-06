package re.notifica.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import re.notifica.Notificare
import re.notifica.internal.NotificareUtils

internal class NotificareHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", NotificareUtils.userAgent)
            .header("X-Notificare-SDK-Version", Notificare.SDK_VERSION)
            .header("X-Notificare-App-Version", NotificareUtils.applicationVersion)
            .build()

        return chain.proceed(request)
    }
}
