package re.notifica.push

import android.content.Intent
import androidx.lifecycle.LiveData
import re.notifica.Notificare
import re.notifica.push.ktx.push

public object NotificarePushCompat {

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

}
