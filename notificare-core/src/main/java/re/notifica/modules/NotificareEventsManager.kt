package re.notifica.modules

import androidx.annotation.RestrictTo
import androidx.work.*
import kotlinx.coroutines.*
import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.NotificareLogger
import re.notifica.internal.common.recoverable
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.internal.storage.database.ktx.toEntity
import re.notifica.internal.workers.ProcessEventsWorker
import re.notifica.models.NotificareEvent
import re.notifica.models.NotificareEventData

class NotificareEventsManager {

    private val discardableEvents = listOf<String>()

    fun configure() {
        // TODO listen to connectivity changes
        // TODO listen to lifecycle changes (app open)
    }

    suspend fun launch() {
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

    fun logNotificationOpened(id: String) {
        log(
            event = "re.notifica.event.notification.Open",
            data = null,
            notificationId = id
        )
    }

    fun logCustom(event: String, data: NotificareEventData? = null) {
        log("re.notifica.event.custom.${event}", data)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun log(event: String, data: NotificareEventData? = null, notificationId: String? = null) {
        GlobalScope.launch {
            val device = Notificare.deviceManager.currentDevice

            try {
                log(
                    NotificareEvent(
                        type = event,
                        timestamp = System.currentTimeMillis(),
                        deviceId = device?.id,
                        sessionId = Notificare.sessionManager.sessionId,
                        notificationId = notificationId,
                        userId = device?.userId,
                        data = data
                    )
                )
            } catch (e: Exception) {
                NotificareLogger.error("Failed to log an event.", e)
            }
        }
    }

    private suspend fun log(event: NotificareEvent): Unit = withContext(Dispatchers.IO) {
        if (!Notificare.isConfigured) {
            NotificareLogger.debug("Notificare is not configured. Skipping event log...")
            return@withContext
        }

        try {
            NotificareRequest.Builder()
                .post("/event", event)
                .response()

            NotificareLogger.info("Event '${event.type}' sent successfully.")
        } catch (e: Exception) {
            NotificareLogger.warning("Failed to send the event: ${event.type}", e)

            if (!discardableEvents.contains(event.type) && e.recoverable) {
                NotificareLogger.info("Queuing event to be sent whenever possible.")
                Notificare.database.events().insert(event.toEntity())
                scheduleUploadWorker()
            }
        }
    }

    private fun scheduleUploadWorker() {
        NotificareLogger.debug("Scheduling a worker to process stored events when there's connectivity.")

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
