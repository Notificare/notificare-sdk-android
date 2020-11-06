package re.notifica.modules

import androidx.work.*
import kotlinx.coroutines.runBlocking
import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.internal.common.recoverable
import re.notifica.internal.storage.database.ktx.toEntity
import re.notifica.internal.workers.ProcessEventsWorker
import re.notifica.models.NotificareEvent
import re.notifica.models.NotificareEventData

class NotificareEventsManager : NotificareModule<Unit>() {

    private val discardableEvents = listOf<String>()

    override fun configure() {
        // TODO listen to connectivity changes
        // TODO listen to lifecycle changes (app open)
    }

    override suspend fun launch() {
        scheduleUploadWorker()
    }

    fun logApplicationInstall() {
        log(NotificareDefinitions.Events.APPLICATION_INSTALL)
    }

    fun logApplicationRegistration() {
        log(NotificareDefinitions.Events.APPLICATION_REGISTRATION)
    }

    fun logApplicationUpgrade() {
        log(NotificareDefinitions.Events.APPLICATION_UPGRADE)
    }

    fun logApplicationOpen() {
        log(NotificareDefinitions.Events.APPLICATION_OPEN)
    }

    fun logApplicationClose(sessionLength: Double) {
        log(
            NotificareDefinitions.Events.APPLICATION_CLOSE, mapOf(
                "length" to sessionLength.toString()
            )
        )
    }

    fun logCustom(event: String, data: NotificareEventData? = null) {
        log("re.notifica.event.custom.${event}", data)
    }

    private fun log(event: String, data: NotificareEventData? = null) = runBlocking {
        val device = Notificare.deviceManager.currentDevice

        try {
            log(
                NotificareEvent(
                    type = event,
                    timestamp = System.currentTimeMillis(),
                    deviceId = device?.deviceId,
                    sessionId = Notificare.sessionManager.sessionId,
                    notificationId = null,
                    userId = device?.userId,
                    data = data
                )
            )
        } catch (e: Exception) {
            Notificare.logger.error("Failed to log an event.", e)
        }
    }

    private suspend fun log(event: NotificareEvent) {
        if (!Notificare.isConfigured) {
            Notificare.logger.debug("Notificare is not configured. Skipping event log...")
            return
        }

        try {
            Notificare.pushService.createEvent(event)
            Notificare.logger.info("Event '${event.type}' sent successfully.")
        } catch (e: Exception) {
            Notificare.logger.warning("Failed to send the event: ${event.type}", e)

            if (!discardableEvents.contains(event.type) && e.recoverable) {
                Notificare.logger.info("Queuing event to be sent whenever possible.")
                Notificare.database.events().insert(event.toEntity())
                scheduleUploadWorker()
            }
        }
    }

    private fun scheduleUploadWorker() {
        Notificare.logger.debug("Scheduling a worker to process stored events when there's connectivity.")

        WorkManager
            .getInstance(Notificare.requireContext())
            .enqueueUniqueWork(
                NotificareDefinitions.Tasks.PROCESS_EVENTS,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<ProcessEventsWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )
    }
}
