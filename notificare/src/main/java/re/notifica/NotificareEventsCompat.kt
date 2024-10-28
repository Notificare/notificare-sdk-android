package re.notifica

import re.notifica.ktx.events
import re.notifica.models.NotificareEventData

public object NotificareEventsCompat {

    /**
     * Logs an application exception, with a callback.
     *
     * This method logs exceptions within the application, helping capture and record critical issues
     * or unusual application states that may affect user experience.
     *
     * @param throwable The exception instance to be logged.
     * @param callback The callback invoked upon completion of the logging operation.
     */
    @JvmStatic
    public fun logApplicationException(throwable: Throwable, callback: NotificareCallback<Unit>) {
        Notificare.events().logApplicationException(throwable, callback)
    }

    /**
     * Logs when a notification has been opened by the user, with a callback.
     *
     * This function records the opening of a notification, enabling insight into user engagement with specific
     * notifications.
     *
     * @param id The unique identifier of the opened notification.
     * @param callback The callback invoked upon completion of the logging operation.
     */
    @JvmStatic
    public fun logNotificationOpen(id: String, callback: NotificareCallback<Unit>) {
        Notificare.events().logNotificationOpen(id, callback)
    }

    /**
     * Logs a custom event in the application, with a callback.
     *
     * This function allows logging of application-specific events.
     *
     * @param event The name of the custom event to log.
     * @param callback The callback invoked upon completion of the logging operation.
     */
    @JvmStatic
    public fun logCustom(event: String, callback: NotificareCallback<Unit>) {
        Notificare.events().logCustom(
            event = event,
            callback = callback
        )
    }

    /**
     * Logs a custom event in the application, with a callback.
     *
     * This function allows logging of application-specific events, optionally associating structured data
     * for more detailed event tracking and analysis.
     *
     * @param event The name of the custom event to log.
     * @param data Optional structured event data for further details.
     * @param callback The callback invoked upon completion of the logging operation.
     */
    @JvmStatic
    public fun logCustom(event: String, data: NotificareEventData?, callback: NotificareCallback<Unit>) {
        Notificare.events().logCustom(event, data, callback)
    }
}
