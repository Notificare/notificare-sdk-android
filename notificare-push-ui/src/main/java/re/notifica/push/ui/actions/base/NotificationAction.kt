package re.notifica.push.ui.actions.base

import android.content.Context
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.models.NotificarePendingResult

abstract class NotificationAction(
    protected val context: Context,
    protected val notification: NotificareNotification,
    protected val action: NotificareNotification.Action,
) {

    abstract suspend fun execute(): NotificarePendingResult?
}
