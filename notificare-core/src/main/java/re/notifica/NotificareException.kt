package re.notifica

sealed class NotificareException(message: String) : Exception(message) {

    object NotReady : NotificareException("Notificare is not ready yet.")
}
