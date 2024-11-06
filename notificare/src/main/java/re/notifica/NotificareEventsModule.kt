package re.notifica

import re.notifica.models.NotificareEventData

public interface NotificareEventsModule {

    /**
     * Logs in Notificare an application exception for diagnostic tracking.
     *
     * This method logs in Notificare exceptions within the application, helping capture and record critical issues
     * that may affect user experience.
     *
     * @param throwable The exception instance to be logged.
     */
    public suspend fun logApplicationException(throwable: Throwable)

    /**
     * Logs in Notificare an application exception, with a callback.
     *
     * This method logs in Notificare exceptions within the application, helping capture and record critical issues
     * that may affect user experience.
     *
     * @param throwable The exception instance to be logged.
     * @param callback The callback invoked upon completion of the logging operation.
     */
    public fun logApplicationException(throwable: Throwable, callback: NotificareCallback<Unit>)

    /**
     * Logs in Notificare when a notification has been opened by the user.
     *
     * This function logs in Notificare the opening of a notification, enabling insight into user engagement with
     * specific notifications.
     *
     * @param id The unique identifier of the opened notification.
     */
    public suspend fun logNotificationOpen(id: String)

    /**
     * Logs in Notificare when a notification has been opened by the user, with a callback.
     *
     * This function logs in Notificare the opening of a notification, enabling insight into user engagement with
     * specific notifications.
     *
     * @param id The unique identifier of the opened notification.
     * @param callback The callback invoked upon completion of the logging operation.
     */
    public fun logNotificationOpen(id: String, callback: NotificareCallback<Unit>)

    /**
     * Logs in Notificare a custom event in the application.
     *
     * This function allows logging, in Notificare, of application-specific events, optionally associating structured
     * data for more detailed event tracking and analysis.
     *
     * @param event The name of the custom event to log.
     * @param data Optional structured event data for further details.
     */
    public suspend fun logCustom(event: String, data: NotificareEventData? = null)

    /**
     * Logs a custom event in the application, with a callback.
     *
     * This function allows logging, in Notificare, of application-specific events, optionally associating structured
     * data for more detailed event tracking and analysis.
     *
     * @param event The name of the custom event to log.
     * @param data Optional structured event data for further details.
     * @param callback The callback invoked upon completion of the logging operation.
     */
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
