package re.notifica.scannables

public sealed class NotificareScannablesException(message: String) : Exception(message) {

    public class UserCancelledScannableSession :
        NotificareScannablesException("The user cancelled the scannable session.")
}
