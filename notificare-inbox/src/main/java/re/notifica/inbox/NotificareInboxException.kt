package re.notifica.inbox

sealed class NotificareInboxException {

    class InboxUnavailable :
        Exception("Inbox functionality is not available for this app. Please configure it via the dashboard.")

    class AutoBadgeUnavailable :
        Exception("Auto badge functionality is not available for this app. Please configure it via the dashboard.")
}
