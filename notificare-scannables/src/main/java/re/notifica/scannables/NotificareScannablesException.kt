package re.notifica.scannables

sealed class NotificareScannablesException(message: String) : Exception(message) {

    class UserCancelledScannableSession : NotificareScannablesException("The user cancelled the scannable session.")
}
