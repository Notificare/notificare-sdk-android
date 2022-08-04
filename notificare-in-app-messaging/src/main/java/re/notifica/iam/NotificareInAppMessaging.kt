package re.notifica.iam

public interface NotificareInAppMessaging {

    public var hasMessagesSuppressed: Boolean

    // TODO: add a Listener for onMessageDisplayed() and onMessageFinished()

    // TODO: add a way to exclude activities from showing in-app messages.
    // Some sensitive flows, like payments, shouldn't pop-up in-app-messages.
}
