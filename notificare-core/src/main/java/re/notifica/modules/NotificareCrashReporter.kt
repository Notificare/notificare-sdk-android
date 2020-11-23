package re.notifica.modules

import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.internal.NotificareUtils
import re.notifica.models.NotificareEvent

class NotificareCrashReporter : NotificareModule<Unit>() {

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private val uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread: Thread, throwable: Throwable ->
        // Save the crash report to be processed when the app recovers.
        saveCrashReport(throwable)
        Notificare.logger.debug("Saved crash report in storage to upload on next start.")

        // Let the app's default handler take over.
        val defaultUncaughtExceptionHandler = defaultUncaughtExceptionHandler ?: run {
            Notificare.logger.warning("Default uncaught exception handler not configured.")
            return@UncaughtExceptionHandler
        }

        defaultUncaughtExceptionHandler.uncaughtException(thread, throwable)
    }

    var enabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value

            setupHandler(value)
        }

    override fun configure() {
        setupHandler(enabled)
    }

    override suspend fun launch() {
        val crashReport = Notificare.sharedPreferences.crashReport ?: run {
            Notificare.logger.debug("No crash report to process.")
            return
        }

        try {
            Notificare.pushService.createEvent(crashReport)
            Notificare.logger.info("Crash report processed.")

            // Clean up the stored crash report
            Notificare.sharedPreferences.crashReport = null
        } catch (e: Exception) {
            Notificare.logger.error("Failed to process a crash report.", e)
        }
    }

    private fun setupHandler(enabled: Boolean) {
        if (enabled) {
            defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
        } else {
            Thread.setDefaultUncaughtExceptionHandler(defaultUncaughtExceptionHandler)
            defaultUncaughtExceptionHandler = null
        }
    }

    private fun saveCrashReport(throwable: Throwable) {
        val timestamp = System.currentTimeMillis()

        Notificare.sharedPreferences.crashReport = NotificareEvent(
            type = NotificareDefinitions.Events.APPLICATION_EXCEPTION,
            timestamp = timestamp,
            deviceId = Notificare.deviceManager.currentDevice?.id,
            sessionId = Notificare.sessionManager.sessionId,
            notificationId = null,
            userId = Notificare.deviceManager.currentDevice?.userId,
            data = mapOf(
                "platform" to "Android",
                "osVersion" to NotificareUtils.osVersion,
                "deviceString" to NotificareUtils.deviceString,
                "sdkVersion" to NotificareDefinitions.SDK_VERSION,
                "appVersion" to NotificareUtils.applicationVersion,
                "timestamp" to timestamp.toString(),
                "name" to throwable.message,
                "reason" to throwable.cause?.toString(),
                "stackSymbols" to throwable.stackTraceToString(),
            )
        )
    }
}
