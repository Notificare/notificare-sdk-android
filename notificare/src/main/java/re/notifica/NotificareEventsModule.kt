package re.notifica

import re.notifica.models.NotificareEventData

public interface NotificareEventsModule {

    public fun logApplicationInstall()

    public fun logApplicationRegistration()

    public fun logApplicationUpgrade()

    public fun logApplicationOpen()

    public fun logApplicationException(throwable: Throwable)

    public fun logApplicationClose(sessionLength: Double)

    public fun logNotificationOpen(id: String)

    public fun logCustom(event: String, data: NotificareEventData? = null)
}

public interface NotificareInternalEventsModule {

    @InternalNotificareApi
    public fun log(event: String, data: NotificareEventData? = null, notificationId: String? = null)
}
