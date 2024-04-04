package re.notifica.iam.ktx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareEventsModule
import re.notifica.iam.models.NotificareInAppMessage

internal suspend fun NotificareEventsModule.logInAppMessageViewed(message: NotificareInAppMessage): Unit =
    withContext(Dispatchers.IO) {
        Notificare.eventsInternal().log(
            event = "re.notifica.event.inappmessage.View",
            data = mapOf(
                "message" to message.id,
            ),
        )
    }

internal suspend fun NotificareEventsModule.logInAppMessageActionClicked(
    message: NotificareInAppMessage,
    action: NotificareInAppMessage.ActionType,
): Unit = withContext(Dispatchers.IO) {
    Notificare.eventsInternal().log(
        event = "re.notifica.event.inappmessage.Action",
        data = mapOf(
            "message" to message.id,
            "action" to action.rawValue,
        ),
    )
}
