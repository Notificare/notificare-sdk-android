package re.notifica.push

import android.content.Intent
import androidx.lifecycle.LiveData
import re.notifica.InternalNotificareApi
import re.notifica.NotificareCallback
import re.notifica.push.models.NotificareTransport
import re.notifica.push.models.NotificareRemoteMessage

public interface NotificarePush {

    public var intentReceiver: Class<out NotificarePushIntentReceiver>

    public val hasRemoteNotificationsEnabled: Boolean

    public val transport: NotificareTransport?

    public val subscriptionId: String?

    public val observableSubscriptionId: LiveData<String?>

    public val allowedUI: Boolean

    public val observableAllowedUI: LiveData<Boolean>

    public suspend fun enableRemoteNotifications()

    public fun enableRemoteNotifications(callback: NotificareCallback<Unit>)

    public fun disableRemoteNotifications()

    // Augmented in the appropriate peer module.
    // public fun isNotificareNotification(...: RemoteMessage)

    public fun handleTrampolineIntent(intent: Intent): Boolean

    public suspend fun registerLiveActivity(activityId: String, topics: List<String> = listOf())

    public fun registerLiveActivity(
        activityId: String,
        topics: List<String> = listOf(),
        callback: NotificareCallback<Unit>,
    )

    public suspend fun endLiveActivity(activityId: String)

    public fun endLiveActivity(activityId: String, callback: NotificareCallback<Unit>)
}

public interface NotificareInternalPush {

    @InternalNotificareApi
    public fun handleNewToken(transport: NotificareTransport, token: String)

    @InternalNotificareApi
    public fun handleRemoteMessage(message: NotificareRemoteMessage)
}
