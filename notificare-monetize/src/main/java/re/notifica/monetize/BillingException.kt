package re.notifica.monetize

public class BillingException public constructor(
    public val code: Int,
    public val debugMessage: String,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
