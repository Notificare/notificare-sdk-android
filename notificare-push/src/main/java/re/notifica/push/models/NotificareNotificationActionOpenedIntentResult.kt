package re.notifica.push.models

import re.notifica.models.NotificareNotification

public data class NotificareNotificationActionOpenedIntentResult(
    val notification: NotificareNotification,
    val action: NotificareNotification.Action,
)
