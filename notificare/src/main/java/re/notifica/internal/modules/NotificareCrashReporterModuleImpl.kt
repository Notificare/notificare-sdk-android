package re.notifica.internal.modules

import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.ktx.eventsImplementation

internal object NotificareCrashReporterModuleImpl : NotificareModule() {

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

    // region Notificare Module

    override fun configure() {
        if (checkNotNull(Notificare.options).crashReportsEnabled) {
            defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
        }
    }

    override suspend fun launch() {
        val crashReport = Notificare.sharedPreferences.crashReport ?: run {
            NotificareLogger.debug("No crash report to process.")
            return
        }

        try {
            Notificare.eventsImplementation().log(crashReport)
            NotificareLogger.info("Crash report processed.")

            // Clean up the stored crash report
            Notificare.sharedPreferences.crashReport = null
        } catch (e: Exception) {
            NotificareLogger.error("Failed to process a crash report.", e)
        }
    }

    // endregion
}
