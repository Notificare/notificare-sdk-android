package re.notifica.push

import android.content.Intent
import androidx.lifecycle.LiveData
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.push.ktx.*

public object NotificarePushCompat {

    // region Intent actions

    @JvmField
    public val INTENT_ACTION_REMOTE_MESSAGE_OPENED: String =
        Notificare.INTENT_ACTION_REMOTE_MESSAGE_OPENED

    @JvmField
    public val INTENT_ACTION_NOTIFICATION_RECEIVED: String =
        Notificare.INTENT_ACTION_NOTIFICATION_RECEIVED

    @JvmField
    public val INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED: String =
        Notificare.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED

    @JvmField
    public val INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED: String =
        Notificare.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED

    @JvmField
    public val INTENT_ACTION_NOTIFICATION_OPENED: String =
        Notificare.INTENT_ACTION_NOTIFICATION_OPENED

    @JvmField
    public val INTENT_ACTION_ACTION_OPENED: String = Notificare.INTENT_ACTION_ACTION_OPENED

    @JvmField
    public val INTENT_ACTION_QUICK_RESPONSE: String = Notificare.INTENT_ACTION_QUICK_RESPONSE

    // endregion

    // region Intent extras

    @JvmField
    public val INTENT_EXTRA_TOKEN: String = Notificare.INTENT_EXTRA_TOKEN

    @JvmField
    public val INTENT_EXTRA_REMOTE_MESSAGE: String = Notificare.INTENT_EXTRA_REMOTE_MESSAGE

    @JvmField
    public val INTENT_EXTRA_TEXT_RESPONSE: String = Notificare.INTENT_EXTRA_TEXT_RESPONSE

    @JvmField
    public val INTENT_EXTRA_LIVE_ACTIVITY_UPDATE: String = Notificare.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE

    // endregion

    @JvmStatic
    public var intentReceiver: Class<out NotificarePushIntentReceiver>
        get() = Notificare.push().intentReceiver
        set(value) {
            Notificare.push().intentReceiver = value
        }

    @JvmStatic
    public val hasRemoteNotificationsEnabled: Boolean
        get() = Notificare.push().hasRemoteNotificationsEnabled

    @JvmStatic
    public val allowedUI: Boolean
        get() = Notificare.push().allowedUI

    @JvmStatic
    public val observableAllowedUI: LiveData<Boolean> = Notificare.push().observableAllowedUI

    @JvmStatic
    public fun enableRemoteNotifications() {
        Notificare.push().enableRemoteNotifications()
    }

    @JvmStatic
    public fun disableRemoteNotifications() {
        Notificare.push().disableRemoteNotifications()
    }

    @JvmStatic
    public fun handleTrampolineIntent(intent: Intent): Boolean {
        return Notificare.push().handleTrampolineIntent(intent)
    }

    @JvmStatic
    public fun registerLiveActivity(
        activityId: String,
        topics: List<String> = listOf(),
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.push().registerLiveActivity(activityId, topics, callback)
    }

    @JvmStatic
    public fun endLiveActivity(
        activityId: String,
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.push().endLiveActivity(activityId, callback)
    }
}
