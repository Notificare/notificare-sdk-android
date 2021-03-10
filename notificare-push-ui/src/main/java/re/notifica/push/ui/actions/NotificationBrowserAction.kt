package re.notifica.push.ui.actions

import android.content.Context
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.actions.base.NotificationAction

class NotificationBrowserAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute() {
        TODO("Not yet implemented")
    }
}
