package re.notifica.internal.network

import okhttp3.Response

sealed class NetworkException(message: String?, cause: Throwable?) : Exception(message, cause) {

    class ParsingException(message: String = "Unable to parse JSON.", cause: Throwable? = null) :
        NetworkException(message, cause)

    class ValidationException(
        val response: Response,
        val validStatusCodes: IntRange,
    ) : NetworkException("Unexpected status code '${response.code}'.", null)
}
