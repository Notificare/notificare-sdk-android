package re.notifica.internal.modules

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.ktx.events
import java.text.SimpleDateFormat
import java.util.*

internal object NotificareSessionModuleImpl : NotificareModule() {

    private val handler = Handler(Looper.getMainLooper())

    private var activityCounter = 0
    private var sessionStart: Date? = null
    private var sessionEnd: Date? = null

    var sessionId: String? = null
        private set

    private val runnable = Runnable {
        // Skip when no session has started. Should never happen.
        val sessionStart = sessionStart ?: return@Runnable
        val sessionEnd = sessionEnd ?: return@Runnable

        Notificare.events().logApplicationClose(
            sessionLength = sessionEnd.time - sessionStart.time / 1000.toDouble()
        )

        this.sessionId = null
        this.sessionStart = null
        this.sessionEnd = null
    }

    // region Notificare Module

    override fun configure() {
        val context = Notificare.requireContext() as Application

        context.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                activityCounter++

                // Cancel any session timeout.
                handler.removeCallbacks(runnable)

                if (sessionStart != null) {
                    NotificareLogger.debug("Resuming previous session.")
                    return
                }

                sessionId = UUID.randomUUID().toString()
                sessionEnd = null
                sessionStart = Date().also {
                    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    NotificareLogger.debug("Session '$sessionId' started at ${format.format(it)}")
                    Notificare.events().logApplicationOpen()
                }
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                activityCounter--

                // Skip when not going into the background.
                if (activityCounter > 0) return

                sessionEnd = Date().also {
                    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    NotificareLogger.debug("Session '$sessionId' stopped at ${format.format(it)}")
                }

                // Wait a few seconds before sending a close event.
                // This prevents quick app swaps, navigation pulls, etc.
                handler.postDelayed(runnable, 10000)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    // endregion
}
