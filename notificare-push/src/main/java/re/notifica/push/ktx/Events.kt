package re.notifica.push.ktx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareEventsModule
import re.notifica.internal.ktx.toCallbackFunction

@Suppress("unused")
public suspend fun NotificareEventsModule.logNotificationReceived(id: String): Unit = withContext(Dispatchers.IO) {
    Notificare.eventsInternal().log(
        event = "re.notifica.event.notification.Receive",
        data = null,
        notificationId = id,
    )
}

public fun NotificareEventsModule.logNotificationReceived(id: String, callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logNotificationReceived)(id, callback)
