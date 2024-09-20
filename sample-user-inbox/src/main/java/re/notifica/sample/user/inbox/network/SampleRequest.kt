package re.notifica.sample.user.inbox.network

import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.EMPTY_REQUEST
import re.notifica.Notificare
import re.notifica.internal.network.NetworkException

internal class SampleRequest private constructor(
    private val request: Request,
    private val validStatusCodes: IntRange,
) {

    private companion object {

        private val HTTP_METHODS_REQUIRE_BODY = arrayOf("PATCH", "POST", "PUT")
        private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()

        private val client = OkHttpClient.Builder().build()
    }

    internal suspend fun response(closeResponse: Boolean): Response = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                handleResponse(
                    response = response,
                    closeResponse = closeResponse,
                    continuation = continuation
                )
            }
        })
    }

    internal suspend fun responseString(): String {
        val response = response(closeResponse = false)

        val body = response.body
            ?: throw IllegalArgumentException("The response contains an empty body. Cannot parse into 'String'.")

        return withContext(Dispatchers.IO) {
            try {
                body.string()
            } catch (e: Exception) {
                throw NetworkException.ParsingException(cause = e)
            } finally {
                body.close()
            }
        }
    }

    private fun handleResponse(response: Response, closeResponse: Boolean, continuation: Continuation<Response>) {
        try {
            if (response.code !in validStatusCodes) {
                response.body?.close()

                continuation.resumeWithException(
                    NetworkException.ValidationException(
                        response = response,
                        validStatusCodes = validStatusCodes,
                    )
                )

                return
            }

            continuation.resume(response)
        } finally {
            if (closeResponse) response.body?.close()
        }
    }

    internal class Builder {

        private var baseUrl: String? = null
        private var url: String? = null
        private var queryItems = mutableMapOf<String, String?>()
        private var headers = mutableMapOf<String, String>()
        private var method: String? = null
        private var body: RequestBody? = null
        private var validStatusCodes: IntRange = 200..299

        internal fun baseUrl(url: String): Builder {
            this.baseUrl = url
            return this
        }

        internal fun get(url: String): Builder = method("GET", url, null)

        internal fun <T : Any> post(url: String, body: T? = null): Builder = method("POST", url, body)

        internal fun <T : Any> put(url: String, body: T? = null): Builder = method("PUT", url, body)

        private fun <T : Any> method(method: String, url: String, body: T?): Builder {
            this.method = method
            this.url = url
            this.body = when (body) {
                null -> if (HTTP_METHODS_REQUIRE_BODY.contains(method)) EMPTY_REQUEST else null
                is ByteArray -> body.toRequestBody()
                is FormBody -> body
                else -> try {
                    val klass = when (body) {
                        // Moshi only supports the default collection types therefore we need to cast them down.
                        //
                        // Concrete example: passing a mapOf() internally uses a LinkedHashMap which would cause
                        // Moshi to throw an IllegalArgumentException since there is no specific adapter registered
                        // for that given type.
                        is Map<*, *> -> Map::class.java
                        else -> body::class.java
                    }

                    val adapter = moshi.adapter<T>(klass)
                    adapter.toJson(body).toRequestBody(MEDIA_TYPE_JSON)
                } catch (e: Exception) {
                    throw NetworkException.ParsingException(message = "Unable to encode body into JSON.", cause = e)
                }
            }

            return this
        }

        internal fun header(name: String, value: String): Builder {
            headers[name] = value
            return this
        }

        internal fun authentication(authentication: String): Builder {
            header("Authorization", authentication)
            return this
        }

        internal fun validate(validStatusCodes: IntRange = 200..299): Builder {
            this.validStatusCodes = validStatusCodes
            return this
        }

        @Throws(IllegalArgumentException::class)
        internal fun build(): SampleRequest {
            val method = requireNotNull(method) { "Please provide the HTTP method for the request." }

            val request = Request.Builder()
                .url(computeCompleteUrl())
                .apply {
                    headers.forEach {
                        header(it.key, it.value)
                    }
                }
                .method(method, body)
                .build()

            return SampleRequest(
                request = request,
                validStatusCodes = validStatusCodes,
            )
        }

        internal suspend fun response(): Response {
            return build().response(true)
        }

        internal suspend fun responseString(): String {
            return build().responseString()
        }

        @Throws(IllegalArgumentException::class)
        private fun computeCompleteUrl(): HttpUrl {
            var url = requireNotNull(url) { "Please provide the URL for the request." }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                var baseUrl = requireNotNull(baseUrl ?: Notificare.servicesInfo?.hosts?.restApi) {
                    "Unable to determine the base url for the request."
                }

                if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                    baseUrl = "https://$baseUrl"
                }

                url = if (!baseUrl.endsWith("/") && !url.startsWith("/")) {
                    "$baseUrl/$url"
                } else {
                    "$baseUrl$url"
                }
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
