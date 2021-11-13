package re.notifica.loyalty.internal.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import re.notifica.Notificare
import re.notifica.loyalty.ktx.loyaltyImplementation

internal class PassRelevanceUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        Notificare.loyaltyImplementation().handleScheduledPassRelevanceUpdate()
        return Result.success()
    }
}
