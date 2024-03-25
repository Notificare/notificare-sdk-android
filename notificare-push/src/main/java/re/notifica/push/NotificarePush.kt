package re.notifica.push

import android.content.Intent
import androidx.lifecycle.LiveData
import re.notifica.InternalNotificareApi
import re.notifica.NotificareCallback
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

    public suspend fun registerLiveActivity(activityId: String, topics: List<String> = listOf(),)

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
    public suspend fun registerPushToken(
        transport: NotificareTransport,
        token: String,
        performReadinessCheck: Boolean = true,
    )

    @InternalNotificareApi
    public fun handleRemoteMessage(message: NotificareRemoteMessage)
}
