package re.notifica.push.ktx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareEventsModule
import re.notifica.utilities.ktx.toCallbackFunction

@Suppress("unused")
public suspend fun NotificareEventsModule.logNotificationReceived(id: String): Unit = withContext(Dispatchers.IO) {
    Notificare.eventsInternal().log(
        event = "re.notifica.event.notification.Receive",
        data = null,
        notificationId = id,
    )
}

public fun NotificareEventsModule.logNotificationReceived(id: String, callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logNotificationReceived)(id, callback::onSuccess, callback::onFailure)

@Suppress("unused")
public suspend fun NotificareEventsModule.logNotificationInfluenced(id: String): Unit = withContext(Dispatchers.IO) {
    Notificare.eventsInternal().log(
        event = "re.notifica.event.notification.Influenced",
        data = null,
        notificationId = id,
    )
}

public fun NotificareEventsModule.logNotificationInfluenced(id: String, callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logNotificationInfluenced)(id, callback::onSuccess, callback::onFailure)

@Suppress("unused")
public suspend fun NotificareEventsModule.logPushRegistration(): Unit = withContext(Dispatchers.IO) {
    Notificare.eventsInternal().log(
        event = "re.notifica.event.push.Registration",
        data = null,
        notificationId = null,
    )
}

public fun NotificareEventsModule.logPushRegistration(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logPushRegistration)(callback::onSuccess, callback::onFailure)
