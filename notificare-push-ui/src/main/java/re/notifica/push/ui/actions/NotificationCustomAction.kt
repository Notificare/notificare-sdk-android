package re.notifica.push.ui.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.app.NotificarePushUIIntentReceiver
import re.notifica.push.ui.models.NotificarePendingResult

class NotificationCustomAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute(): NotificarePendingResult? {
        val uri = action.target?.let { Uri.parse(it) }

        if (uri != null && uri.scheme != null && uri.host != null) {
            val intent = Intent()
                .setAction(NotificarePushUIIntentReceiver.INTENT_ACTION_CUSTOM_ACTION)
                .setClass(Notificare.requireContext(), NotificarePushUI.intentReceiver)
                .setData(uri)

            context.sendBroadcast(intent)

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
