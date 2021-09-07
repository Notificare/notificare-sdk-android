package re.notifica.inbox

public sealed class NotificareInboxException {

    public class InboxUnavailable :
        Exception("Inbox functionality is not available for this app. Please configure it via the dashboard.")

    public class AutoBadgeUnavailable :
        Exception("Auto badge functionality is not available for this app. Please configure it via the dashboard.")
}
