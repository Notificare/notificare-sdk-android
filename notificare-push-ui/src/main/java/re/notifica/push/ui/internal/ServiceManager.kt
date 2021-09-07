package re.notifica.push.ui.internal

import androidx.fragment.app.Fragment
import re.notifica.InternalNotificareApi
import re.notifica.internal.AbstractServiceManager
import re.notifica.models.NotificareNotification

@InternalNotificareApi
public abstract class ServiceManager : AbstractServiceManager() {

    public abstract fun getFragmentClass(notification: NotificareNotification): Class<out Fragment>

    internal companion object {
        private const val FCM_FQN = "re.notifica.push.ui.fcm.internal.ServiceManager"
        private const val HMS_FQN = "re.notifica.push.ui.hms.internal.ServiceManager"

        internal fun create(): ServiceManager {
            return Factory.create(
                gms = FCM_FQN,
                hms = HMS_FQN
            )
        }
    }
}
