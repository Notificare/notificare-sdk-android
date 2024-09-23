package re.notifica.internal.modules

import androidx.annotation.Keep
import re.notifica.Notificare
import re.notifica.utilities.logging.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.ktx.device
import re.notifica.ktx.eventsImplementation

@Keep
internal object NotificareCrashReporterModuleImpl : NotificareModule() {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "NotificareCrashReporter"
    )

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private val uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread: Thread, throwable: Throwable ->
        val device = Notificare.device().currentDevice ?: run {
            logger.warning("Cannot process a crash report before the device becomes available.")
            return@UncaughtExceptionHandler
        }

        // Save the crash report to be processed when the app recovers.
        val event = Notificare.eventsImplementation().createThrowableEvent(throwable, device)
        Notificare.sharedPreferences.crashReport = event
        logger.debug("Saved crash report in storage to upload on next start.")

        // Let the app's default handler take over.
        val defaultUncaughtExceptionHandler = defaultUncaughtExceptionHandler ?: run {
            logger.warning("Default uncaught exception handler not configured.")
            return@UncaughtExceptionHandler
        }

        defaultUncaughtExceptionHandler.uncaughtException(thread, throwable)
    }

    // region Notificare Module

    override fun configure() {
        if (checkNotNull(Notificare.options).crashReportsEnabled) {
            defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
        }
    }

    override suspend fun launch() {
        val crashReport = Notificare.sharedPreferences.crashReport ?: run {
            logger.debug("No crash report to process.")
            return
        }

        try {
            Notificare.eventsImplementation().log(crashReport)
            logger.info("Crash report processed.")

            // Clean up the stored crash report
            Notificare.sharedPreferences.crashReport = null
        } catch (e: Exception) {
            logger.error("Failed to process a crash report.", e)
        }
    }

    // endregion
}
