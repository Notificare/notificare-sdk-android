package re.notifica.iam.internal

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.Keep
import kotlinx.coroutines.*
import re.notifica.Notificare
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.iam.NotificareInAppMessaging
import re.notifica.iam.backgroundGracePeriodMillis
import re.notifica.iam.internal.caching.NotificareImageCache
import re.notifica.iam.internal.network.push.InAppMessageResponse
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.iam.ui.InAppMessagingActivity
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.common.onMainThread
import re.notifica.internal.ktx.activityInfo
import re.notifica.internal.ktx.coroutineScope
import re.notifica.internal.network.NetworkException
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import java.lang.ref.WeakReference

@Keep
internal object NotificareInAppMessagingImpl : NotificareModule(), NotificareInAppMessaging {

    private const val MANIFEST_SUPPRESS_MESSAGES_ACTIVITY_KEY = "re.notifica.iam.ui.suppress_messages"
    private const val DEFAULT_BACKGROUND_GRACE_PERIOD_MILLIS = 5 * 60 * 1000L

    private var foregroundActivitiesCounter = 0
    private var currentState: ApplicationState = ApplicationState.BACKGROUND
    private var currentActivity: WeakReference<Activity>? = null
    private var delayedMessageJob: Job? = null
    private var isShowingMessage = false
    private var backgroundTimestamp: Long? = null

    internal val lifecycleListeners = mutableListOf<NotificareInAppMessaging.MessageLifecycleListener>()

    // region Notificare Module

    override suspend fun launch() {
        evaluateContext(ApplicationContext.LAUNCH)
    }

    // endregion

    // region Notificare In App Messaging

    override var hasMessagesSuppressed: Boolean = false

    override fun setMessagesSuppressed(suppressed: Boolean, evaluateContext: Boolean) {
        val suppressChanged = suppressed != hasMessagesSuppressed
        val canEvaluate = evaluateContext && suppressChanged && !suppressed

        hasMessagesSuppressed = suppressed

        if (canEvaluate) {
            evaluateContext(ApplicationContext.FOREGROUND)
        }
    }

    override fun addLifecycleListener(listener: NotificareInAppMessaging.MessageLifecycleListener) {
        lifecycleListeners.add(listener)
    }

    override fun removeLifecycleListener(listener: NotificareInAppMessaging.MessageLifecycleListener) {
        lifecycleListeners.remove(listener)
    }

    // endregion

    internal fun setupLifecycleListeners(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                currentActivity = WeakReference(activity)
                foregroundActivitiesCounter++

                // No need to run the check when we already processed the foreground check.
                if (currentState != ApplicationState.FOREGROUND) {
                    onApplicationForeground(activity)
                }

                if (activity is InAppMessagingActivity) {
                    // Keep track of the in-app message being displayed.
                    // This will occur when the activity is started.
                    isShowingMessage = true
                }
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                foregroundActivitiesCounter--

                // Skip when not going into the background.
                if (foregroundActivitiesCounter > 0) return

                currentState = ApplicationState.BACKGROUND
                backgroundTimestamp = System.currentTimeMillis()

                if (delayedMessageJob != null) {
                    NotificareLogger.info("Clearing delayed in-app message from being presented when going to the background.")
                    delayedMessageJob?.cancel()
                    delayedMessageJob = null
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity is InAppMessagingActivity) {
                    isShowingMessage = false
                }
            }
        })
    }

    internal fun hasExpiredBackgroundPeriod(timestamp: Long): Boolean {
        val backgroundGracePeriodMillis = Notificare.options?.backgroundGracePeriodMillis
            ?: DEFAULT_BACKGROUND_GRACE_PERIOD_MILLIS

        return System.currentTimeMillis() > timestamp + backgroundGracePeriodMillis
    }

    private fun onApplicationForeground(activity: Activity) {
        val backgroundTimestamp = backgroundTimestamp
        val hasExpiredBackgroundPeriod = backgroundTimestamp != null &&
            hasExpiredBackgroundPeriod(backgroundTimestamp)

        currentState = ApplicationState.FOREGROUND
        this.backgroundTimestamp = null

        if (hasExpiredBackgroundPeriod) {
            NotificareLogger.debug("The current in-app message should have been dismissed for being in the background for longer than the grace period.")
            isShowingMessage = false
        }

        if (!Notificare.isReady) {
            NotificareLogger.debug("Postponing in-app message evaluation until Notificare is launched.")
            return
        }

        if (isShowingMessage) {
            NotificareLogger.debug("Skipping context evaluation since there is another in-app message being presented.")
            return
        }

        if (hasMessagesSuppressed) {
            NotificareLogger.debug("Skipping context evaluation since in-app messages are being suppressed.")
            return
        }

        val packageManager = Notificare.requireContext().packageManager

        val info = packageManager.activityInfo(activity.componentName, PackageManager.GET_META_DATA)
        if (info.metaData != null) {
            val suppressed = info.metaData.getBoolean(MANIFEST_SUPPRESS_MESSAGES_ACTIVITY_KEY, false)
            if (suppressed) {
                NotificareLogger.debug("Skipping context evaluation since in-app messages on ${activity::class.java.simpleName} are being suppressed.")
                return
            }
        }

        evaluateContext(ApplicationContext.FOREGROUND)
    }

    private fun evaluateContext(context: ApplicationContext) {
        NotificareLogger.debug("Checking in-app message for context '${context.rawValue}'.")

        Notificare.coroutineScope.launch {
            try {
                val message = fetchInAppMessage(context)
                processInAppMessage(message)
            } catch (e: Exception) {
                if (e is NetworkException.ValidationException && e.response.code == 404) {
                    NotificareLogger.debug("There is no in-app message for '${context.rawValue}' context to process.")

                    if (context == ApplicationContext.LAUNCH) {
                        evaluateContext(ApplicationContext.FOREGROUND)
                    }

                    return@launch
                }

                NotificareLogger.error("Failed to process in-app message for context '${context.rawValue}'.", e)
            }
        }
    }

    private fun processInAppMessage(message: NotificareInAppMessage) {
        NotificareLogger.info("Processing in-app message '${message.name}'.")

        if (message.delaySeconds > 0) {
            // Keep a reference to the job to cancel it when
            // the app goes into the background.
            delayedMessageJob = Notificare.coroutineScope.launch {
                try {
                    NotificareLogger.debug("Waiting ${message.delaySeconds} seconds before presenting the in-app message.")
                    delay(message.delaySeconds * 1000L)
                    present(message)
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        NotificareLogger.debug("The delayed in-app message job has been canceled.")
                        return@launch
                    }

                    NotificareLogger.error("Failed to present the delayed in-app message.", e)

                    onMainThread {
                        lifecycleListeners.forEach { it.onMessageFailedToPresent(message) }
                    }
                }
            }

            delayedMessageJob?.invokeOnCompletion {
                // Clear the reference to the Job upon its completion.
                delayedMessageJob = null
            }

            return
        }

        present(message)
    }

    private fun present(message: NotificareInAppMessage) {
        Notificare.coroutineScope.launch {
            if (NotificareImageCache.isLoading) {
                NotificareLogger.debug("Cannot display an in-app message while another is being preloaded.")
                return@launch
            }

            try {
                NotificareImageCache.preloadImages(Notificare.requireContext(), message)
            } catch (e: Exception) {
                NotificareLogger.error("Failed to preload the in-app message images.", e)

                onMainThread {
                    lifecycleListeners.forEach { it.onMessageFailedToPresent(message) }
                }

                return@launch
            }

            if (isShowingMessage) {
                NotificareLogger.warning("Cannot display an in-app message while another is being presented.")

                onMainThread {
                    lifecycleListeners.forEach { it.onMessageFailedToPresent(message) }
                }

                return@launch
            }

            if (hasMessagesSuppressed) {
                NotificareLogger.debug("Cannot display an in-app message while messages are being suppressed.")

                onMainThread {
                    lifecycleListeners.forEach { it.onMessageFailedToPresent(message) }
                }

                return@launch
            }

            val activity = currentActivity?.get() ?: run {
                NotificareLogger.warning("Cannot display an in-app message without a reference to the current activity.")

                onMainThread {
                    lifecycleListeners.forEach { it.onMessageFailedToPresent(message) }
                }

                return@launch
            }

            activity.runOnUiThread {
                try {
                    NotificareLogger.debug("Presenting in-app message '${message.name}'.")
                    InAppMessagingActivity.show(activity, message)

                    onMainThread {
                        lifecycleListeners.forEach { it.onMessagePresented(message) }
                    }
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to present the in-app message.", e)

                    onMainThread {
                        lifecycleListeners.forEach { it.onMessageFailedToPresent(message) }
                    }
                }
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
