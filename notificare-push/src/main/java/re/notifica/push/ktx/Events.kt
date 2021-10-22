package re.notifica.push.ktx

import re.notifica.Notificare
import re.notifica.NotificareEventsModule

@Suppress("unused")
public fun NotificareEventsModule.logNotificationReceived(id: String) {
    Notificare.eventsInternal().log(
        event = "re.notifica.event.notification.Receive",
        data = null,
        notificationId = id
    )
}
