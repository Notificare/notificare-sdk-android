package re.notifica.push.ui.actions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.R
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.models.NotificarePendingResult

class NotificationMailAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute(): NotificarePendingResult? {
        val target = action.target

        if (target != null) {
            val intent = Intent(Intent.ACTION_SEND)
                .setType("message/rfc822")
                .putExtra(Intent.EXTRA_EMAIL, arrayOf(target))

            if (context !is Activity) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        context.resources.getText(R.string.notificare_action_title_intent_email)
                    )
                )
            } catch (e: ActivityNotFoundException) {
                // callback.onError(NotificareError(R.string.notificare_action_error_no_email_clients))
                return null
            }

            Notificare.createNotificationReply(notification, action)

            // NotificarePushUI.shared.delegate?.notificare(NotificarePushUI.shared, didExecuteAction: self.action, for: self.notification)
            // Notificare.shared.sendNotificationReply(self.action, for: self.notification) { _ in }
        } else {
            // NotificarePushUI.shared.delegate?.notificare(NotificarePushUI.shared, didFailToExecuteAction: action, for: notification, error: ActionError.invalidUrl)
            // callback.onError(new NotificareError(R.string.notificare_action_failed));
        }

        return null
    }
}
