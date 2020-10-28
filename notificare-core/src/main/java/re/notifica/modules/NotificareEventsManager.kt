package re.notifica.modules

import androidx.work.*
import com.squareup.moshi.Types
import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.internal.NotificareUtils
import re.notifica.internal.common.recoverable
import re.notifica.internal.storage.database.entities.NotificareEventEntity
import re.notifica.internal.workers.ProcessEventsWorker
import re.notifica.models.NotificareEvent
import re.notifica.models.NotificareEventData

class NotificareEventsManager {

    private val moshi = NotificareUtils.createMoshi()
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

    suspend fun logCustom(event: String, data: NotificareEventData? = null) {
        log("re.notifica.event.custom.${event}", data)
    }

    private suspend fun log(event: String, data: NotificareEventData? = null) {
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
            Notificare.logger.warning("Failed to send the event: ${event.type}", e)

            if (!discardableEvents.contains(event.type) && e.recoverable) {
                Notificare.logger.info("Queuing event to be sent whenever possible.")

                val adapter = moshi.adapter<Map<String, String?>>(
                    Types.newParameterizedType(
                        Map::class.java,
                        String::class.java,
                        String::class.java
                    )
                )

                Notificare.database.events().insert(
                    NotificareEventEntity(
                        type = event.type,
                        timestamp = event.timestamp,
                        deviceId = event.deviceId,
                        sessionId = event.sessionId,
                        notificationId = event.notificationId,
                        userId = event.userId,
                        data = adapter.toJson(event.data),
                        retries = 0,
                        ttl = 86400, // 24 hours
                    )
                )

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
