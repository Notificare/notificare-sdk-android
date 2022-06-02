package re.notifica.push.ui.actions

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.R
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.ktx.pushUIImplementation
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.models.NotificarePendingResult

internal class NotificationInAppBrowserAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute(): NotificarePendingResult? = withContext(Dispatchers.IO) {
        val uri = action.target?.let { Uri.parse(it) }

        if (uri != null) {
            Notificare.pushUIImplementation().createInAppBrowser().launchUrl(context, uri)

            Notificare.createNotificationReply(notification, action)

            withContext(Dispatchers.Main) {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.onActionExecuted(notification, action)
                }
            }
        } else {
            throw Exception(context.getString(R.string.notificare_action_failed))
        }

        return@withContext null
    }
}
