package re.notifica.push

import re.notifica.NotificareEventsManager

fun NotificareEventsManager.logNotificationReceived(id: String) {
    log(
        event = "re.notifica.event.notification.Receive",
        data = null,
        notificationId = id
    )
}
