package re.notifica.internal.network

import okhttp3.Interceptor
import okhttp3.Response
import re.notifica.Notificare
import re.notifica.internal.NotificareUtils

internal class NotificareHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-Notificare-SDK-Version", Notificare.SDK_VERSION)
            .addHeader("X-Notificare-App-Version", NotificareUtils.applicationVersion)
            .build()

        return chain.proceed(request)
    }
}
