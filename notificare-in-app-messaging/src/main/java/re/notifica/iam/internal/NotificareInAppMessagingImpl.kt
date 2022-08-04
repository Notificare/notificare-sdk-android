package re.notifica.iam.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.iam.NotificareInAppMessaging
import re.notifica.iam.internal.network.push.InAppMessageResponse
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.iam.ui.InAppMessagingActivity
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.network.NetworkException
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import java.lang.ref.WeakReference

@Keep
internal object NotificareInAppMessagingImpl : NotificareModule(), NotificareInAppMessaging {

    private var foregroundActivitiesCounter = 0
    private var currentState: ApplicationState = ApplicationState.BACKGROUND
    private var currentActivity: WeakReference<Activity>? = null

    override suspend fun launch() {
        evaluateContext(ApplicationContext.LAUNCH)
    }

    internal fun setupLifecycleListeners(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                foregroundActivitiesCounter++

                if (currentState == ApplicationState.FOREGROUND) return

                currentState = ApplicationState.FOREGROUND

                if (!Notificare.isReady) {
                    NotificareLogger.debug("Postponing in-app message evaluation until Notificare is launched.")
                    return
                }

                evaluateContext(ApplicationContext.FOREGROUND)
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                foregroundActivitiesCounter--

                // Skip when not going into the background.
                if (foregroundActivitiesCounter > 0) return

                currentState = ApplicationState.BACKGROUND
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun evaluateContext(context: ApplicationContext) {
        NotificareLogger.debug("Checking in-app message for context '${context.rawValue}'.")

        GlobalScope.launch {
            try {
                val message = fetchInAppMessage(context)
                processInAppMessage(message)
            } catch (e: Exception) {
                if (e is NetworkException.ValidationException && e.response.code == 404) {
                    NotificareLogger.debug("There is no in-app message for '${context.rawValue}' context to process.")

                    if (context == ApplicationContext.LAUNCH) {
                        evaluateContext(ApplicationContext.FOREGROUND)
                        return@launch
                    }
                } else {
                    NotificareLogger.error("Failed to process in-app message for context '${context.rawValue}'.", e)
                }
            }
        }
    }

    private fun processInAppMessage(message: NotificareInAppMessage) {
        NotificareLogger.info("Processing in-app message '${message.name}'.")

        val activity = currentActivity?.get() ?: run {
            NotificareLogger.warning("Cannot display an in-app message without a reference to the current activity.")
            return
        }

        activity.runOnUiThread {
            try {
                InAppMessagingActivity.show(activity, message)
            } catch (e: Exception) {
                NotificareLogger.error("Failed to add the in-app message view to the window.", e)
            }
        }
    }

    private suspend fun fetchInAppMessage(
        context: ApplicationContext
    ): NotificareInAppMessage = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        NotificareRequest.Builder()
            .get("/inappmessage/forcontext/${context.rawValue}")
            .query("deviceID", device.id)
            .responseDecodable(InAppMessageResponse::class)
            .message
            .toModel()
    }


    // TODO: checkPrerequisites()
}
