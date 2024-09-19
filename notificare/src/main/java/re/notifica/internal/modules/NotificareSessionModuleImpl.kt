package re.notifica.internal.modules

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.Keep
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.utilities.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.utilities.ktx.notificareCoroutineScope
import re.notifica.ktx.device
import re.notifica.ktx.eventsImplementation

@Keep
internal object NotificareSessionModuleImpl : NotificareModule() {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "NotificareSession"
    )

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        notificareCoroutineScope.launch {
            try {
                stopSession()
            } catch (e: Exception) {
                // Silent.
            }
        }
    }

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
        logger.debug("Session '$sessionId' started at ${format.format(sessionStart)}")

        try {
            Notificare.eventsImplementation().logApplicationOpen(
                sessionId = sessionId,
            )
        } catch (e: Exception) {
            logger.warning("Failed to process an application session start.")
        }
    }

    private suspend fun stopSession() = withContext(Dispatchers.IO) {
        // Skip when no session has started. Should never happen.
        val sessionId = sessionId ?: return@withContext
        val sessionStart = sessionStart ?: return@withContext
        val sessionEnd = sessionEnd ?: return@withContext

        this@NotificareSessionModuleImpl.sessionId = null
        this@NotificareSessionModuleImpl.sessionStart = null
        this@NotificareSessionModuleImpl.sessionEnd = null

        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        logger.debug("Session '$sessionId' stopped at ${format.format(sessionEnd)}")

        try {
            Notificare.eventsImplementation().logApplicationClose(
                sessionId = sessionId,
                sessionLength = (sessionEnd.time - sessionStart.time) / 1000.toDouble(),
            )
        } catch (e: Exception) {
            logger.warning("Failed to process an application session stop.")
        }
    }

    internal fun setupLifecycleListeners(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // no-op
            }

            override fun onActivityStarted(activity: Activity) {
                if (activityCounter == 0) {
                    logger.debug("Resuming previous session.")
                }

                activityCounter++

                // Cancel any session timeout.
                handler.removeCallbacks(runnable)

                // Prevent multiple session starts.
                if (sessionId != null) return

                if (!Notificare.isReady) {
                    logger.debug("Postponing session start until Notificare is launched.")
                    return
                }

                notificareCoroutineScope.launch {
                    try {
                        startSession()
                    } catch (e: Exception) {
                        // Silent.
                    }
                }
            }

            override fun onActivityResumed(activity: Activity) {
                // no-op
            }

            override fun onActivityPaused(activity: Activity) {
                // no-op
            }

            override fun onActivityStopped(activity: Activity) {
                activityCounter--

                // Skip when not going into the background.
                if (activityCounter > 0) return

                sessionEnd = Date()

                // Wait a few seconds before sending a close event.
                // This prevents quick app swaps, navigation pulls, etc.
                handler.postDelayed(runnable, 10000)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                // no-op
            }

            override fun onActivityDestroyed(activity: Activity) {
                // no-op
            }
        })
    }

    // region Notificare Module

    override suspend fun launch() {
        if (sessionId == null && Notificare.device().currentDevice != null) {
            // Launch is taking place after the first activity has been created.
            // Start the application session.
            startSession()
        }
    }

    override suspend fun unlaunch() {
        sessionEnd = Date()
        stopSession()
    }

    // endregion
}
