package re.notifica.internal.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import re.notifica.Notificare
import re.notifica.utilities.logging.NotificareLogger
import re.notifica.utilities.networking.isRecoverable
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.internal.storage.database.entities.NotificareEventEntity
import re.notifica.internal.storage.database.ktx.toModel

private const val MAX_RETRIES = 5

internal class ProcessEventsWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "ProcessEventWorker"
    )

    override suspend fun doWork(): Result {
        return try {
            Notificare.database.events().find().forEach { processEvent(it) }

            logger.debug("Finished processing all the events.")
            Result.success()
        } catch (e: Exception) {
            logger.warning("Failed to process the stored events.", e)
            return Result.failure()
        }
    }

    private suspend fun processEvent(entity: NotificareEventEntity) {
        logger.debug("Processing event #${entity.id}")

        val now = Date()
        val createdAt = Date(entity.timestamp)
        val expiresAt = GregorianCalendar()
            .let { calendar ->
                calendar.time = createdAt
                calendar.add(Calendar.SECOND, entity.ttl)
                calendar.time
            }

        if (now.after(expiresAt)) {
            logger.debug("Event expired. Removing...")
            Notificare.database.events().delete(entity)
            return
        }

        try {
            NotificareRequest.Builder()
                .post("/event", entity.toModel())
                .response()

            logger.debug("Event processed. Removing from storage...")
            Notificare.database.events().delete(entity)
        } catch (e: Exception) {
            if (e.isRecoverable) {
                logger.debug("Failed to process event.")

                // Increase the attempts counter.
                entity.retries++

                if (entity.retries < MAX_RETRIES) {
                    // Persist the attempts counter.
                    Notificare.database.events().update(entity)
                } else {
                    logger.debug("Event was retried too many times. Removing...")
                    Notificare.database.events().delete(entity)
                }
            } else {
                logger.debug("Failed to process event due to an unrecoverable error. Discarding it...")
                Notificare.database.events().delete(entity)
            }
        }
    }
}
