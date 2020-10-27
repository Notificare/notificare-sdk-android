package re.notifica.modules

import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.models.NotificareEvent

class NotificareEventsManager {

    private val discardableEvents = listOf<String>()

    internal fun configure() {
        // TODO listen to connectivity changes
        // TODO listen to lifecycle changes (app open)
    }

    internal fun launch() {
        // TODO process stored events
    }

    suspend fun logApplicationInstall() {
        log(NotificareDefinitions.Events.APPLICATION_INSTALL)
    }

    suspend fun logApplicationRegistration() {
        log(NotificareDefinitions.Events.APPLICATION_REGISTRATION)
    }

    suspend fun logApplicationUpgrade() {
        log(NotificareDefinitions.Events.APPLICATION_UPGRADE)
    }

    suspend fun logApplicationOpen() {
        log(NotificareDefinitions.Events.APPLICATION_OPEN)
    }

    suspend fun logApplicationClose() {
        // TODO log the session length
        log(NotificareDefinitions.Events.APPLICATION_CLOSE)
    }

    suspend fun logCustom(event: String, data: Map<String, Any>) {
        log("re.notifica.event.custom.${event}", data)
    }

    private suspend fun log(event: String, data: Map<String, Any>? = null) {
        val device = Notificare.deviceManager.currentDevice ?: run {
            Notificare.logger.warning("Cannot send an event before a device is registered.")
            return
        }

        log(
            NotificareEvent(
                type = event,
                timestamp = System.currentTimeMillis(),
                deviceId = device.deviceId,
                sessionId = null, // TODO me
                notificationId = null,
                userId = device.userId,
                data = data
            )
        )
    }

    private suspend fun log(event: NotificareEvent) {
        if (!Notificare.isReady) {
            Notificare.logger.debug("Notificare is not ready. Skipping event log...")
            return
        }

        try {
            Notificare.pushService.createEvent(event)
            Notificare.logger.info("Event sent successfully.")
        } catch (e: Exception) {
            Notificare.logger.error("Failed to send the event: ${event.type}", e)

            // TODO check if the error is recoverable
            if (!discardableEvents.contains(event.type)) {
                // TODO save in the database to process later.
            }
        }
    }
}
