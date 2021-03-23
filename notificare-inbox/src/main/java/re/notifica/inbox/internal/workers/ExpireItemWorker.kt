package re.notifica.inbox.internal.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import re.notifica.inbox.NotificareInbox

internal class ExpireItemWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = coroutineScope {
        inputData.getString(PARAM_ITEM_ID)?.run {
            NotificareInbox.handleExpiredItem(this)
        }

        Result.success()
    }

    companion object {
        const val PARAM_ITEM_ID = "re.notifica.worker.param.InboxItemId"
    }
}
