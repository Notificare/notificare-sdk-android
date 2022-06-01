package re.notifica.internal.modules

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.ktx.eventsImplementation
import java.text.SimpleDateFormat
import java.util.*

internal object NotificareSessionModuleImpl : NotificareModule() {

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable(::stopSession)

    private var activityCounter = 0
    private var sessionStart: Date? = null
    private var sessionEnd: Date? = null

    var sessionId: String? = null
        private set


    private suspend fun startSession() = withContext(Dispatchers.IO) {
        val sessionId = UUID.randomUUID().toString()
        val sessionStart = Date()

        this@NotificareSessionModuleImpl.sessionId = sessionId
        this@NotificareSessionModuleImpl.sessionEnd = null
        this@NotificareSessionModuleImpl.sessionStart = sessionStart

        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        NotificareLogger.debug("Session '$sessionId' started at ${format.format(sessionStart)}")

        try {
            Notificare.eventsImplementation().logApplicationOpen(
                sessionId = sessionId,
            )
        } catch (e: Exception) {
            NotificareLogger.warning("Failed to process an application session start.")
        }
    }

    private fun stopSession() {
        // Skip when no session has started. Should never happen.
        val sessionId = sessionId ?: return
        val sessionStart = sessionStart ?: return
        val sessionEnd = sessionEnd ?: return

        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        NotificareLogger.debug("Session '$sessionId' stopped at ${format.format(sessionEnd)}")

        GlobalScope.launch {
            try {
                Notificare.eventsImplementation().logApplicationClose(
                    sessionId = sessionId,
                    sessionLength = sessionEnd.time - sessionStart.time / 1000.toDouble(),
                )
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to process an application session stop.")
            }
        }

        this.sessionId = null
        this.sessionStart = null
        this.sessionEnd = null
    }

    internal fun setupLifecycleListeners(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                activityCounter++.also {
                    if (it == 1) {
                        NotificareLogger.debug("Resuming previous session.")
                    }
                }

                // Cancel any session timeout.
                handler.removeCallbacks(runnable)

                // Prevent multiple session starts.
                if (sessionId != null || sessionStart != null) return

                if (!Notificare.isReady) {
                    NotificareLogger.debug("Postponing session start until Notificare is launched.")
                    return
                }

                GlobalScope.launch {
                    try {
                        startSession()
                    } catch (e: Exception) {
                        // Silent.
                    }
                }
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                activityCounter--

                // Skip when not going into the background.
                if (activityCounter > 0) return

                sessionEnd = Date()

                // Wait a few seconds before sending a close event.
                // This prevents quick app swaps, navigation pulls, etc.
                handler.postDelayed(runnable, 10000)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    // region Notificare Module

    override suspend fun launch() {
        if (activityCounter > 0 && sessionId == null && sessionStart == null) {
            startSession()
        }
    }

    // endregion
}
