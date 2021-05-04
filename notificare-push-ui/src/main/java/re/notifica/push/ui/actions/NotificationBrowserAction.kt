package re.notifica.push.ui.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.models.NotificarePendingResult

class NotificationBrowserAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute(): NotificarePendingResult? = withContext(Dispatchers.IO) {
        val uri = action.target?.let { Uri.parse(it) }

        if (uri != null && uri.scheme != null && uri.host != null) {
            val intent = Intent(Intent.ACTION_VIEW, uri)

            if (context !is Activity) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)

            Notificare.createNotificationReply(notification, action)

            // NotificarePushUI.shared.delegate?.notificare(NotificarePushUI.shared, didExecuteAction: self.action, for: self.notification)
            // Notificare.shared.sendNotificationReply(self.action, for: self.notification) { _ in }
        } else {
            // NotificarePushUI.shared.delegate?.notificare(NotificarePushUI.shared, didFailToExecuteAction: action, for: notification, error: ActionError.invalidUrl)
            // callback.onError(new NotificareError(R.string.notificare_action_failed));
        }

        return@withContext null
    }
}
