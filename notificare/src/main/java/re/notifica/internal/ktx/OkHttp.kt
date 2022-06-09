package re.notifica.internal.ktx

import okhttp3.Request

internal fun Request.Builder.unsafeHeader(name: String, value: String): Request.Builder {
    return headers(
        build()
            .headers
            .newBuilder()
            .addUnsafeNonAscii(name, value)
            .build()
    )
}
