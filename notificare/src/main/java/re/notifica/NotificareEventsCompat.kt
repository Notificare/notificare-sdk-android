package re.notifica

import re.notifica.ktx.events
import re.notifica.models.NotificareEventData

public object NotificareEventsCompat {

    @JvmStatic
    public fun logApplicationException(throwable: Throwable, callback: NotificareCallback<Unit>) {
        Notificare.events().logApplicationException(throwable, callback)
    }

    @JvmStatic
    public fun logNotificationOpen(id: String, callback: NotificareCallback<Unit>) {
        Notificare.events().logNotificationOpen(id, callback)
    }

    @JvmStatic
    public fun logCustom(event: String, callback: NotificareCallback<Unit>) {
        Notificare.events().logCustom(
            event = event,
            callback = callback
        )
    }

    @JvmStatic
    public fun logCustom(
        event: String,
        data: NotificareEventData?,
        callback: NotificareCallback<Unit>
    ) {
        Notificare.events().logCustom(event, data, callback)
    }
}
