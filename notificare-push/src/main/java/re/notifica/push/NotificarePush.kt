package re.notifica.push

import android.content.Intent
import androidx.lifecycle.LiveData
import com.google.firebase.messaging.RemoteMessage
import re.notifica.InternalNotificareApi
import re.notifica.NotificareCallback
import re.notifica.push.models.NotificarePushSubscription
import re.notifica.push.models.NotificareTransport
import re.notifica.push.models.NotificareRemoteMessage

public interface NotificarePush {

    public var intentReceiver: Class<out NotificarePushIntentReceiver>

    public val hasRemoteNotificationsEnabled: Boolean

    public val transport: NotificareTransport?

    public val subscription: NotificarePushSubscription?

    public val observableSubscription: LiveData<NotificarePushSubscription?>

    public val allowedUI: Boolean

    public val observableAllowedUI: LiveData<Boolean>

    public suspend fun enableRemoteNotifications()

    public fun enableRemoteNotifications(callback: NotificareCallback<Unit>)

    public suspend fun disableRemoteNotifications()

    public fun disableRemoteNotifications(callback: NotificareCallback<Unit>)

    public fun isNotificareNotification(remoteMessage: RemoteMessage): Boolean

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

internal interface NotificareInternalPush {

    @InternalNotificareApi
    fun handleNewToken(transport: NotificareTransport, token: String)

    @InternalNotificareApi
    fun handleRemoteMessage(message: NotificareRemoteMessage)
}
