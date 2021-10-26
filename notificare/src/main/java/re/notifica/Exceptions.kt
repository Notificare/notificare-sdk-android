package re.notifica

public class NotificareNotConfiguredException
    : Exception("Notificare hasn't been configured. Call Notificare.configure() or enable the configuration provider.")

public class NotificareNotReadyException
    : Exception("Notificare is not ready. Call Notificare.launch() and wait for the 'ready' event.")

public class NotificareDeviceUnavailableException
    : Exception("Notificare device unavailable at the moment. It becomes available after the first ready event.")

public class NotificareApplicationUnavailableException
    : Exception("Notificare application unavailable at the moment. It becomes available after the first ready event.")

public class NotificareServiceUnavailableException(
    public val service: String,
) : Exception("Notificare '$service' service is not available. Check the dashboard and documentation to enable it.")
