package re.notifica.internal.network.request

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_REQUEST
import okhttp3.logging.HttpLoggingInterceptor
import re.notifica.BuildConfig
import re.notifica.Notificare
import re.notifica.internal.NotificareUtils
import re.notifica.internal.network.NetworkException
import re.notifica.internal.network.push.NotificareBasicAuthenticator
import re.notifica.internal.network.push.NotificareHeadersInterceptor
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

class NotificareRequest private constructor(
    private val request: Request,
    private val validStatusCodes: IntRange,
) {

    companion object {

        private val HTTP_METHODS_REQUIRE_BODY = arrayOf("PATCH", "POST", "PUT")
        private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()

        private val moshi = NotificareUtils.createMoshi()

        private val client = OkHttpClient.Builder()
            .authenticator(NotificareBasicAuthenticator())
            .addInterceptor(NotificareHeadersInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .build()
    }

    suspend fun response(): Response = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code !in validStatusCodes) {
                    continuation.resumeWithException(
                        NetworkException.ValidationException(
                            response = response,
                            validStatusCodes = validStatusCodes,
                        )
                    )

                    return
                }

                continuation.resume(response)
            }
        })
    }

    suspend fun <T : Any> responseDecodable(klass: KClass<T>): T {
        val response = response()

        val body = response.body
            ?: throw IllegalArgumentException("The response contains an empty body. Cannot parse into '${klass.simpleName}'.")

        return withContext(Dispatchers.IO) {
            try {
                val adapter = moshi.adapter(klass.java)

                @Suppress("BlockingMethodInNonBlockingContext")
                adapter.fromJson(body.source())
                    ?: throw NetworkException.ParsingException(message = "JSON parsing resulted in a null object.")
            } catch (e: Exception) {
                throw NetworkException.ParsingException(cause = e)
            }
        }
    }

    class Builder {

        private var baseUrl: String? = null
        private var url: String? = null
        private var queryItems = mutableMapOf<String, String?>()
        private var method: String? = null
        private var body: RequestBody? = null
        private var validStatusCodes: IntRange = 200..299

        fun baseUrl(url: String): Builder {
            this.baseUrl = url
            return this
        }

        fun get(url: String): Builder = method("GET", url, null)

        fun <T : Any> patch(url: String, body: T? = null): Builder = method("PATCH", url, body)

        fun <T : Any> post(url: String, body: T? = null): Builder = method("POST", url, body)

        fun <T : Any> put(url: String, body: T? = null): Builder = method("PUT", url, body)

        fun <T : Any> delete(url: String, body: T? = null): Builder = method("DELETE", url, body)

        private fun <T : Any> method(method: String, url: String, body: T?): Builder {
            this.method = method
            this.url = url
            this.body = when (body) {
                null -> if (HTTP_METHODS_REQUIRE_BODY.contains(method)) EMPTY_REQUEST else null
                else -> try {
                    val klass = body::class.java
                    val adapter = moshi.adapter<T>(klass)

                    adapter.toJson(body).toRequestBody(MEDIA_TYPE_JSON)
                } catch (e: Exception) {
                    throw NetworkException.ParsingException(message = "Unable to encode body into JSON.", cause = e)
                }
            }

            return this
        }

        fun query(items: Map<String, String?>): Builder {
            queryItems.putAll(items)
            return this
        }

        fun query(item: Pair<String, String?>): Builder {
            queryItems[item.first] = item.second
            return this
        }

        fun query(name: String, value: String?): Builder {
            queryItems[name] = value
            return this
        }

        fun validate(validStatusCodes: IntRange = 200..299): Builder {
            this.validStatusCodes = validStatusCodes
            return this
        }


        @Throws(IllegalArgumentException::class)
        fun build(): NotificareRequest {
            val method = requireNotNull(method) { "Please provide the HTTP method for the request." }

            val request = Request.Builder()
                .url(computeCompleteUrl())
                .method(method, body)
                .build()

            return NotificareRequest(request = request, validStatusCodes = validStatusCodes)
        }

        suspend fun response(): Response {
            return build().response()
        }

        suspend fun <T : Any> responseDecodable(klass: KClass<T>): T {
            return build().responseDecodable(klass)
        }

        @Throws(IllegalArgumentException::class)
        private fun computeCompleteUrl(): HttpUrl {
            var url = requireNotNull(url) { "Please provide the URL for the request." }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                val baseUrl = requireNotNull(baseUrl ?: Notificare.servicesConfig?.pushHost) {
                    "Unable to determine the base url for the request."
                }

                if (!baseUrl.endsWith("/") && !url.startsWith("/")) {
                    url = "$baseUrl/$url"
                }

                url = "$baseUrl$url"
            }

            if (queryItems.isNotEmpty()) {
                return url.toHttpUrl()
                    .newBuilder()
                    .apply {
                        queryItems.forEach { (key, value) -> addQueryParameter(key, value) }
                    }
                    .build()
            }

            return url.toHttpUrl()
        }
    }
}
