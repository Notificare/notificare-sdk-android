@file:Suppress("detekt:MatchingDeclarationName")

package re.notifica.push

@Suppress("detekt:MaxLineLength")
public class NotificareSubscriptionUnavailable: Exception(
    "Notificare push subscription unavailable at the moment. It becomes available after calling enableRemoteNotifications()."
)
