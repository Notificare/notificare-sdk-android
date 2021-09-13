package re.notifica

public sealed class NotificareException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    public class NotReady : NotificareException("Notificare is not ready yet.")

    public class Unknown(cause: Throwable) : NotificareException("Unknown exception occurred.", cause)
}
