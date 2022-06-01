package re.notifica

import re.notifica.models.NotificareEventData

public interface NotificareEventsModule {

    public suspend fun logApplicationException(throwable: Throwable)

    public fun logApplicationException(throwable: Throwable, callback: NotificareCallback<Unit>)

    public suspend fun logNotificationOpen(id: String)

    public fun logNotificationOpen(id: String, callback: NotificareCallback<Unit>)

    public suspend fun logCustom(event: String, data: NotificareEventData? = null)

    public fun logCustom(event: String, data: NotificareEventData? = null, callback: NotificareCallback<Unit>)
}

public interface NotificareInternalEventsModule {

    @InternalNotificareApi
    public suspend fun log(
        event: String,
        data: NotificareEventData? = null,
        sessionId: String? = null,
        notificationId: String? = null,
    )
}
