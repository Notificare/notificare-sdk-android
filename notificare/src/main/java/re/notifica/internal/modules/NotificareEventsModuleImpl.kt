package re.notifica.internal.modules

import androidx.work.*
import kotlinx.coroutines.*
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareEventsModule
import re.notifica.NotificareInternalEventsModule
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareUtils
import re.notifica.internal.common.recoverable
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.internal.storage.database.ktx.toEntity
import re.notifica.internal.workers.ProcessEventsWorker
import re.notifica.ktx.device
import re.notifica.ktx.session
import re.notifica.models.NotificareEvent
import re.notifica.models.NotificareEventData

private const val EVENT_APPLICATION_INSTALL = "re.notifica.event.application.Install"
private const val EVENT_APPLICATION_REGISTRATION = "re.notifica.event.application.Registration"
private const val EVENT_APPLICATION_UPGRADE = "re.notifica.event.application.Upgrade"
private const val EVENT_APPLICATION_OPEN = "re.notifica.event.application.Open"
private const val EVENT_APPLICATION_CLOSE = "re.notifica.event.application.Close"
private const val EVENT_APPLICATION_EXCEPTION = "re.notifica.event.application.Exception"
private const val EVENT_NOTIFICATION_OPEN = "re.notifica.event.notification.Open"
private const val TASK_UPLOAD_EVENTS = "re.notifica.tasks.events.Upload"

internal object NotificareEventsModuleImpl : NotificareModule(), NotificareEventsModule,
    NotificareInternalEventsModule {

    private val discardableEvents = listOf<String>()

    // region Notificare Module

//    override fun configure() {
//        // TODO listen to connectivity changes
//        // TODO listen to lifecycle changes (app open)
//    }

    override suspend fun launch() {
        scheduleUploadWorker()
    }

    // endregion

    // region Notificare Events Module

    override suspend fun logApplicationInstall() {
        log(EVENT_APPLICATION_INSTALL)
    }

    override fun logApplicationInstall(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logApplicationInstall)(callback)

    override suspend fun logApplicationRegistration() {
        log(EVENT_APPLICATION_REGISTRATION)
    }

    override fun logApplicationRegistration(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logApplicationRegistration)(callback)

    override suspend fun logApplicationUpgrade() {
        log(EVENT_APPLICATION_UPGRADE)
    }

    override fun logApplicationUpgrade(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logApplicationUpgrade)(callback)

    override suspend fun logApplicationOpen() {
        log(EVENT_APPLICATION_OPEN)
    }

    override fun logApplicationOpen(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logApplicationOpen)(callback)

    override suspend fun logApplicationException(throwable: Throwable) {
        log(throwable.toEvent())
    }

    override fun logApplicationException(throwable: Throwable, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logApplicationException)(throwable, callback)

    override suspend fun logApplicationClose(sessionLength: Double) {
        log(
            EVENT_APPLICATION_CLOSE, mapOf(
                "length" to sessionLength.toString()
            )
        )
    }

    override fun logApplicationClose(sessionLength: Double, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logApplicationClose)(sessionLength, callback)

    override suspend fun logNotificationOpen(id: String) {
        log(
            event = EVENT_NOTIFICATION_OPEN,
            data = null,
            notificationId = id
        )
    }

    override fun logNotificationOpen(id: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logNotificationOpen)(id, callback)

    override suspend fun logCustom(event: String, data: NotificareEventData?) {
        log("re.notifica.event.custom.$event", data)
    }

    override fun logCustom(event: String, data: NotificareEventData?, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logCustom)(event, data, callback)

    // endregion

    // region Notificare Internal Events Module

    override suspend fun log(event: String, data: NotificareEventData?, notificationId: String?) {
        log(
            NotificareEvent(
                type = event,
                timestamp = System.currentTimeMillis(),
                deviceId = Notificare.device().currentDevice?.id,
                sessionId = Notificare.session().sessionId,
                notificationId = notificationId,
                userId = Notificare.device().currentDevice?.userId,
                data = data
            )
        )
    }

    // endregion

    internal suspend fun log(event: NotificareEvent): Unit = withContext(Dispatchers.IO) {
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

                return@withContext
            }

            throw e
        }
    }

    private fun scheduleUploadWorker() {
        NotificareLogger.debug("Scheduling a worker to process stored events when there's connectivity.")

        WorkManager
            .getInstance(Notificare.requireContext())
            .enqueueUniqueWork(
                TASK_UPLOAD_EVENTS,
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

internal fun Throwable.toEvent(): NotificareEvent {
    val timestamp = System.currentTimeMillis()

    return NotificareEvent(
        type = EVENT_APPLICATION_EXCEPTION,
        timestamp = timestamp,
        deviceId = Notificare.device().currentDevice?.id,
        sessionId = Notificare.session().sessionId,
        notificationId = null,
        userId = Notificare.device().currentDevice?.userId,
        data = mapOf(
            "platform" to "Android",
            "osVersion" to NotificareUtils.osVersion,
            "deviceString" to NotificareUtils.deviceString,
            "sdkVersion" to Notificare.SDK_VERSION,
            "appVersion" to NotificareUtils.applicationVersion,
            "timestamp" to timestamp.toString(),
            "name" to this.message,
            "reason" to this.cause?.toString(),
            "stackSymbols" to this.stackTraceToString(),
        )
    )
}
