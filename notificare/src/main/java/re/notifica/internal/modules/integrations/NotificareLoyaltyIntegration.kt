package re.notifica.internal.modules.integrations

import android.app.Activity
import re.notifica.InternalNotificareApi
import re.notifica.NotificareCallback
import re.notifica.models.NotificareNotification

@InternalNotificareApi
public interface NotificareLoyaltyIntegration {

    public fun handlePassPresentation(
        activity: Activity,
        notification: NotificareNotification,
        callback: NotificareCallback<Unit>,
    )
}
