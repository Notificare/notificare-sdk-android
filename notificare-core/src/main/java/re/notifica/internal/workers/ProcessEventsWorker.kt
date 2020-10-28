package re.notifica.internal.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import re.notifica.Notificare

internal class ProcessEventsWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    // TODO add actual implementation
    override suspend fun doWork(): Result {
        Notificare.logger.debug("do work")

        Notificare.database.events().find().forEach {
            Notificare.logger.debug("Event #${it.id}: ${it.type}")
        }

        Notificare.logger.debug("work done")

        return Result.success()
    }
}
