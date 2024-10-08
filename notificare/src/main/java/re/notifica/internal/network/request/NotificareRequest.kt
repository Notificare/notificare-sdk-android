package re.notifica.internal.network.request

import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
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
import okhttp3.logging.HttpLoggingInterceptor
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.internal.logger
import re.notifica.internal.moshi
import re.notifica.internal.network.NetworkException
import re.notifica.internal.network.NotificareHeadersInterceptor
import re.notifica.utilities.coroutines.toCallbackFunction

@InternalNotificareApi
public typealias DecodableForFn<T> = (responseCode: Int) -> KClass<T>?

@InternalNotificareApi
public class NotificareRequest private constructor(
    private val request: Request,
    private val validStatusCodes: IntRange,
) {

    private companion object {

        private val HTTP_METHODS_REQUIRE_BODY = arrayOf("PATCH", "POST", "PUT")
        private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()

        private val client = OkHttpClient.Builder()
            // .authenticator(NotificareBasicAuthenticator())
            .addInterceptor(NotificareHeadersInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (Notificare.options?.debugLoggingEnabled == true) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
    }

    public suspend fun response(closeResponse: Boolean): Response = suspendCoroutine { continuation ->
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

    public suspend fun responseString(): String {
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

    public suspend fun <T : Any> responseDecodable(klass: KClass<T>): T {
        val response = response(closeResponse = false)

        val body = response.body
            ?: throw IllegalArgumentException(
                "The response contains an empty body. Cannot parse into '${klass.simpleName}'."
            )

        return withContext(Dispatchers.IO) {
            try {
                val adapter = Notificare.moshi.adapter(klass.java)

                adapter.fromJson(body.source())
                    ?: throw NetworkException.ParsingException(message = "JSON parsing resulted in a null object.")
            } catch (e: Exception) {
                throw NetworkException.ParsingException(cause = e)
            } finally {
                body.close()
            }
        }
    }

    public suspend fun <T : Any> responseDecodable(
        decodableFor: DecodableForFn<T>,
    ): T? = withContext(Dispatchers.IO) {
        response(closeResponse = false).use { response ->
            val klass = decodableFor(response.code)
                ?: return@withContext null

            val body = response.body
                ?: throw IllegalArgumentException(
                    "The response contains an empty body. Cannot parse into '${klass.simpleName}'."
                )

            try {
                val adapter = Notificare.moshi.adapter(klass.java)

                adapter.fromJson(body.source())
                    ?: throw NetworkException.ParsingException(message = "JSON parsing resulted in a null object.")
            } catch (e: Exception) {
                throw NetworkException.ParsingException(cause = e)
            }
        }
    }

    private fun handleResponse(response: Response, closeResponse: Boolean, continuation: Continuation<Response>) {
        try {
            if (response.code !in validStatusCodes) {
                // Forcefully close the body. Decodable responses will not proceed.
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

    public class Builder {
        private var baseUrl: String? = null
        private var url: String? = null
        private var queryItems = mutableMapOf<String, String?>()
        private var authentication: Authentication? = createDefaultAuthentication()
        private var headers = mutableMapOf<String, String>()
        private var method: String? = null
        private var body: RequestBody? = null
        private var validStatusCodes: IntRange = 200..299

        public fun baseUrl(url: String): Builder {
            this.baseUrl = url
            return this
        }

        public fun get(url: String): Builder = method("GET", url, null)

        public fun <T : Any> patch(url: String, body: T? = null): Builder = method("PATCH", url, body)

        public fun <T : Any> post(url: String, body: T? = null): Builder = method("POST", url, body)

        public fun <T : Any> put(url: String, body: T? = null): Builder = method("PUT", url, body)

        public fun <T : Any> delete(url: String, body: T? = null): Builder = method("DELETE", url, body)

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

                    val adapter = Notificare.moshi.adapter<T>(klass)
                    adapter.toJson(body).toRequestBody(MEDIA_TYPE_JSON)
                } catch (e: Exception) {
                    throw NetworkException.ParsingException(message = "Unable to encode body into JSON.", cause = e)
                }
            }

            return this
        }

        public fun query(items: Map<String, String?>): Builder {
            queryItems.putAll(items)
            return this
        }

        public fun query(item: Pair<String, String?>): Builder {
            queryItems[item.first] = item.second
            return this
        }

        public fun query(name: String, value: String?): Builder {
            queryItems[name] = value
            return this
        }

        public fun header(name: String, value: String): Builder {
            headers[name] = value
            return this
        }

        public fun authentication(authentication: Authentication?): Builder {
            this.authentication = authentication
            return this
        }

        public fun validate(validStatusCodes: IntRange = 200..299): Builder {
            this.validStatusCodes = validStatusCodes
            return this
        }

        @Throws(IllegalArgumentException::class)
        public fun build(): NotificareRequest {
            val method = requireNotNull(method) { "Please provide the HTTP method for the request." }

            val request = Request.Builder()
                .url(computeCompleteUrl())
                .apply {
                    headers.forEach {
                        header(it.key, it.value)
                    }

                    authentication?.run {
                        header("Authorization", encode())
                    }
                }
                .method(method, body)
                .build()

            return NotificareRequest(
                request = request,
                validStatusCodes = validStatusCodes,
            )
        }

        public suspend fun response(): Response {
            return build().response(true)
        }

        public fun response(callback: NotificareCallback<Response>): Unit =
            toCallbackFunction(::response)(callback::onSuccess, callback::onFailure)

        public suspend fun responseString(): String {
            return build().responseString()
        }

        public fun responseString(callback: NotificareCallback<String>): Unit =
            toCallbackFunction(::responseString)(callback::onSuccess, callback::onFailure)

        public suspend fun <T : Any> responseDecodable(klass: KClass<T>): T {
            return build().responseDecodable(klass)
        }

        public fun <T : Any> responseDecodable(klass: KClass<T>, callback: NotificareCallback<T>): Unit =
            toCallbackFunction(suspend { responseDecodable(klass) })(callback::onSuccess, callback::onFailure)

        public suspend fun <T : Any> responseDecodable(decodableFor: DecodableForFn<T>): T? {
            return build().responseDecodable(decodableFor)
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

        private fun createDefaultAuthentication(): Authentication? {
            val key = Notificare.servicesInfo?.applicationKey
            val secret = Notificare.servicesInfo?.applicationSecret

            if (key == null || secret == null) {
                logger.warning("Notificare application authentication not configured.")
                return null
            }

            return Authentication.Basic(key, secret)
        }
    }

    public sealed class Authentication {
        public abstract fun encode(): String

        public data class Basic(
            val username: String,
            val password: String,
        ) : Authentication() {

            override fun encode(): String {
                return Credentials.basic(username, password)
            }
        }
    }
}
