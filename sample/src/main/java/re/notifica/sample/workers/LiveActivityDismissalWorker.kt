package re.notifica.sample.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import re.notifica.sample.live_activities.LiveActivitiesController
import re.notifica.sample.live_activities.LiveActivity

class CoffeeBrewerDismissalWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            LiveActivitiesController.notificationManager.activeNotifications
                .filter { it.tag == LiveActivity.COFFEE_BREWER.identifier }
                .forEach {
                    LiveActivitiesController.notificationManager.cancel(
                        LiveActivity.COFFEE_BREWER.identifier,
                        it.id
                    )
                }
            Result.success()

        } catch (e: Exception) {
            Result.failure()
        }
    }
}
