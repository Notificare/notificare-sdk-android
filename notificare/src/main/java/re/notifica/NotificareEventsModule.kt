package re.notifica

import re.notifica.models.NotificareEventData

public interface NotificareEventsModule {

    public suspend fun logApplicationInstall()

    public fun logApplicationInstall(callback: NotificareCallback<Unit>)

    public suspend fun logApplicationRegistration()

    public fun logApplicationRegistration(callback: NotificareCallback<Unit>)

    public suspend fun logApplicationUpgrade()

    public fun logApplicationUpgrade(callback: NotificareCallback<Unit>)

    public suspend fun logApplicationOpen()

    public fun logApplicationOpen(callback: NotificareCallback<Unit>)

    public suspend fun logApplicationException(throwable: Throwable)

    public fun logApplicationException(throwable: Throwable, callback: NotificareCallback<Unit>)

    public suspend fun logApplicationClose(sessionLength: Double)

    public fun logApplicationClose(sessionLength: Double, callback: NotificareCallback<Unit>)

    public suspend fun logNotificationOpen(id: String)

    public fun logNotificationOpen(id: String, callback: NotificareCallback<Unit>)

    public suspend fun logCustom(event: String, data: NotificareEventData? = null)

    public fun logCustom(event: String, data: NotificareEventData? = null, callback: NotificareCallback<Unit>)
}

public interface NotificareInternalEventsModule {

    @InternalNotificareApi
    public suspend fun log(event: String, data: NotificareEventData? = null, notificationId: String? = null)
}
