package re.notifica.internal

import re.notifica.Notificare
import re.notifica.NotificareEventsManager
import re.notifica.models.NotificareEvent

internal class CrashReporter {

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private val uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread: Thread, throwable: Throwable ->
        // Save the crash report to be processed when the app recovers.
        Notificare.sharedPreferences.crashReport = throwable.toEvent()
        NotificareLogger.debug("Saved crash report in storage to upload on next start.")

        // Let the app's default handler take over.
        val defaultUncaughtExceptionHandler = defaultUncaughtExceptionHandler ?: run {
            NotificareLogger.warning("Default uncaught exception handler not configured.")
            return@UncaughtExceptionHandler
        }

        defaultUncaughtExceptionHandler.uncaughtException(thread, throwable)
    }

    internal fun configure() {
        if (checkNotNull(Notificare.options).crashReportsEnabled) {
            defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
        }
    }

    internal suspend fun launch() {
        val crashReport = Notificare.sharedPreferences.crashReport ?: run {
            NotificareLogger.debug("No crash report to process.")
            return
        }

        try {
            Notificare.eventsManager.log(crashReport)
            NotificareLogger.info("Crash report processed.")

            // Clean up the stored crash report
            Notificare.sharedPreferences.crashReport = null
        } catch (e: Exception) {
            NotificareLogger.error("Failed to process a crash report.", e)
        }
    }
}

internal fun Throwable.toEvent(): NotificareEvent {
    val timestamp = System.currentTimeMillis()

    return NotificareEvent(
        type = NotificareEventsManager.EVENT_APPLICATION_EXCEPTION,
        timestamp = timestamp,
        deviceId = Notificare.deviceManager.currentDevice?.id,
        sessionId = Notificare.sessionManager.sessionId,
        notificationId = null,
        userId = Notificare.deviceManager.currentDevice?.userId,
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
