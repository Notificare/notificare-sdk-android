package re.notifica.push

import android.content.Intent
import androidx.lifecycle.LiveData
import re.notifica.InternalNotificareApi
import re.notifica.models.NotificareTransport
import re.notifica.push.models.NotificareRemoteMessage

public interface NotificarePush {

    public var intentReceiver: Class<out NotificarePushIntentReceiver>

    public val hasRemoteNotificationsEnabled: Boolean

    public val allowedUI: Boolean

    public val observableAllowedUI: LiveData<Boolean>

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
