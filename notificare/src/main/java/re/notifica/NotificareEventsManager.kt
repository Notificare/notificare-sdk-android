package re.notifica

import androidx.work.*
import kotlinx.coroutines.*
import re.notifica.internal.common.recoverable
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.internal.storage.database.ktx.toEntity
import re.notifica.internal.workers.ProcessEventsWorker
import re.notifica.models.NotificareEvent
import re.notifica.models.NotificareEventData

public class NotificareEventsManager {

    public companion object {
        internal const val EVENT_APPLICATION_INSTALL = "re.notifica.event.application.Install"
        internal const val EVENT_APPLICATION_REGISTRATION = "re.notifica.event.application.Registration"
        internal const val EVENT_APPLICATION_UPGRADE = "re.notifica.event.application.Upgrade"
        internal const val EVENT_APPLICATION_OPEN = "re.notifica.event.application.Open"
        internal const val EVENT_APPLICATION_CLOSE = "re.notifica.event.application.Close"
        internal const val EVENT_APPLICATION_EXCEPTION = "re.notifica.event.application.Exception"
        internal const val EVENT_NOTIFICATION_OPEN = "re.notifica.event.notification.Open"

        internal const val TASK_PROCESS_EVENTS = "re.notifica.tasks.process_events"
    }

    private val discardableEvents = listOf<String>()

    internal fun configure() {
        // TODO listen to connectivity changes
        // TODO listen to lifecycle changes (app open)
    }

    internal fun launch() {
        scheduleUploadWorker()
    }

    public fun logApplicationInstall() {
        log(EVENT_APPLICATION_INSTALL)
    }

    public fun logApplicationRegistration() {
        log(EVENT_APPLICATION_REGISTRATION)
    }

    public fun logApplicationUpgrade() {
        log(EVENT_APPLICATION_UPGRADE)
    }

    public fun logApplicationOpen() {
        log(EVENT_APPLICATION_OPEN)
    }

    public fun logApplicationClose(sessionLength: Double) {
        log(
            EVENT_APPLICATION_CLOSE, mapOf(
                "length" to sessionLength.toString()
            )
        )
    }

    public fun logNotificationOpened(id: String) {
        log(
            event = EVENT_NOTIFICATION_OPEN,
            data = null,
            notificationId = id
        )
    }

    public fun logCustom(event: String, data: NotificareEventData? = null) {
        log("re.notifica.event.custom.$event", data)
    }

    @InternalNotificareApi
    public fun log(event: String, data: NotificareEventData? = null, notificationId: String? = null) {
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
                TASK_PROCESS_EVENTS,
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
