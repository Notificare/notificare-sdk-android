package re.notifica.sample

import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import re.notifica.push.NotificarePushIntentReceiver
import re.notifica.push.models.NotificareLiveActivityUpdate
import re.notifica.push.models.NotificarePushSubscription
import re.notifica.sample.live_activities.LiveActivitiesController
import re.notifica.sample.live_activities.LiveActivity
import re.notifica.sample.live_activities.models.CoffeeBrewerContentState
import re.notifica.sample.workers.CoffeeBrewerDismissalWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SamplePushIntentReceiver : NotificarePushIntentReceiver() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val liveActivitiesController = LiveActivitiesController

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            INTENT_ACTION_COFFEE_BREWER_DISMISS -> dismissLiveActivity(LiveActivity.COFFEE_BREWER)
        }
    }

    override fun onSubscriptionChanged(context: Context, subscription: NotificarePushSubscription?) {
        coroutineScope.launch {
            try {
                liveActivitiesController.handleSubscriptionChanged(subscription)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update registered live activities.")
            }
        }
    }

    override fun onLiveActivityUpdate(context: Context, update: NotificareLiveActivityUpdate) {
        coroutineScope.launch {
            try {
                when (LiveActivity.from(update.activity)) {
                    LiveActivity.COFFEE_BREWER -> {
                        val contentState = update.content<CoffeeBrewerContentState>()
                            ?: return@launch

                        liveActivitiesController.updateCoffeeActivity(contentState)

                        if (update.final) {
                            var delay = DEFAULT_DISMISSAL_MILLISECONDS
                            val dismissalDate = update.dismissalDate

                            if (dismissalDate != null) {
                                delay = if (dismissalDate.time <= System.currentTimeMillis()) {
                                    0
                                } else {
                                    dismissalDate.time - System.currentTimeMillis()
                                }
                            }

                            val request = OneTimeWorkRequestBuilder<CoffeeBrewerDismissalWorker>()
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .build()

                            WorkManager.getInstance(context).enqueue(request)

                            LiveActivitiesController.updateCoffeeBrewerState(null)
                        }
                    }

                    null -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the live activity.")
            }
        }
    }

    private fun dismissLiveActivity(activity: LiveActivity) {
        coroutineScope.launch {
            try {
                when (activity) {
                    LiveActivity.COFFEE_BREWER -> liveActivitiesController.clearCoffeeActivity()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to end the live activity.")
            }
        }
    }

    companion object {
        const val INTENT_ACTION_COFFEE_BREWER_DISMISS =
            "re.notifica.sample.intent.action.CoffeeBrewerDismiss"

        private const val DEFAULT_DISMISSAL_MILLISECONDS: Long = 4 * 60 * 60 * 1000
    }
}
