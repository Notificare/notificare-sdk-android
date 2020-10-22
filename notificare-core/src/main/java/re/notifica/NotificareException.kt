package re.notifica

sealed class NotificareException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    object NotReady : NotificareException("Notificare is not ready yet.")

    class Unknown(cause: Throwable) : NotificareException("Unknown exception occurred.", cause)
}
