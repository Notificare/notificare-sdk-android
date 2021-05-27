package re.notifica.push.ui.actions

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.R
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.models.NotificarePendingResult

class NotificationCustomAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute(): NotificarePendingResult? = withContext(Dispatchers.IO) {
        val uri = action.target?.let { Uri.parse(it) }

        if (uri != null && uri.scheme != null && uri.host != null) {
            NotificarePushUI.lifecycleListeners.forEach { it.onCustomActionReceived(uri) }

            Notificare.createNotificationReply(notification, action)
            NotificarePushUI.lifecycleListeners.forEach { it.onActionExecuted(notification, action) }
        } else {
            throw Exception(context.getString(R.string.notificare_action_failed))
        }

        return@withContext null
    }
}
