package re.notifica.push

import android.content.Intent
import re.notifica.InternalNotificareApi
import re.notifica.models.NotificareTransport
import re.notifica.push.models.NotificareRemoteMessage

public interface NotificarePush {

    public val INTENT_ACTION_NOTIFICATION_OPENED: String
        get() = "re.notifica.intent.action.NotificationOpened"

    public val INTENT_ACTION_ACTION_OPENED: String
        get() = "re.notifica.intent.action.ActionOpened"

    public var intentReceiver: Class<out NotificarePushIntentReceiver>

    public val hasRemoteNotificationsEnabled: Boolean

    public val allowedUI: Boolean

    public fun enableRemoteNotifications()

    public fun disableRemoteNotifications()

    // Augmented in the appropriate peer module.
    // public fun isNotificareNotification(...: RemoteMessage)

    public fun handleTrampolineIntent(intent: Intent): Boolean
}

public interface NotificareInternalPush {
    @InternalNotificareApi
    public var postponedDeviceToken: String?

    @InternalNotificareApi
    public suspend fun registerPushToken(transport: NotificareTransport, token: String)

    @InternalNotificareApi
    public fun handleRemoteMessage(message: NotificareRemoteMessage)
}
