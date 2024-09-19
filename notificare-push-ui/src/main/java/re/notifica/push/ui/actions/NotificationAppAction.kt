package re.notifica.push.ui.actions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.utilities.NotificareLogger
import re.notifica.utilities.onMainThread
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.R
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.models.NotificarePendingResult

internal class NotificationAppAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "NotificationAppAction"
    )

    override suspend fun execute(): NotificarePendingResult? = withContext(Dispatchers.IO) {
        val uri = action.target?.let { Uri.parse(it) }

        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW, uri)

            if (context !is Activity) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                logger.debug("No activity found to handle the action.", e)
                throw Exception(context.getString(R.string.notificare_action_failed))
            }

            Notificare.createNotificationReply(notification, action)

            onMainThread {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.get()?.onActionExecuted(
                        notification,
                        action
                    )
                }
            }
        } else {
            throw Exception(context.getString(R.string.notificare_action_failed))
        }

        return@withContext null
    }
}
