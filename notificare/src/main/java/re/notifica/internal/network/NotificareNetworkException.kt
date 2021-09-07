package re.notifica.internal.network

import okhttp3.Response

public sealed class NetworkException(message: String?, cause: Throwable?) : Exception(message, cause) {

    public class ParsingException(message: String = "Unable to parse JSON.", cause: Throwable? = null) :
        NetworkException(message, cause)

    public class ValidationException(
        public val response: Response,
        public val validStatusCodes: IntRange,
    ) : NetworkException("Unexpected status code '${response.code}'.", null)
}
