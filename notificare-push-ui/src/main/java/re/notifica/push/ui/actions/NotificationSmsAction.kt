package re.notifica.push.ui.actions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.R
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.models.NotificarePendingResult

internal class NotificationSmsAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute(): NotificarePendingResult? = withContext(Dispatchers.IO) {
        val target = action.target

        if (target != null) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", target, null))

            if (context !is Activity) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        context.resources.getText(R.string.notificare_action_title_intent_sms)
                    )
                )
            } catch (e: ActivityNotFoundException) {
                throw Exception(context.getString(R.string.notificare_action_error_no_sms_clients))
            }

            Notificare.createNotificationReply(notification, action)

            withContext(Dispatchers.Main) {
                NotificarePushUI.lifecycleListeners.forEach { it.onActionExecuted(notification, action) }
            }
        } else {
            throw Exception(context.getString(R.string.notificare_action_failed))
        }

        return@withContext null
    }
}
