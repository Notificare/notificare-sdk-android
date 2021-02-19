package re.notifica.push

import re.notifica.modules.NotificareEventsManager

fun NotificareEventsManager.logNotificationReceived(id: String) {
    log(
        event = "re.notifica.event.notification.Receive",
        data = null,
        notificationId = id
    )
}

fun NotificareEventsManager.logNotificationOpened(id: String) {
    log(
        event = "re.notifica.event.notification.Open",
        data = null,
        notificationId = id
    )
}
