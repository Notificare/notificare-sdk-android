package re.notifica.push.internal

import re.notifica.InternalNotificareApi
import re.notifica.internal.ServiceManager
import re.notifica.models.NotificareTransport

@InternalNotificareApi
public abstract class PushServiceManager : ServiceManager() {

    public abstract val transport: NotificareTransport

    public abstract fun requestPushToken()

    internal companion object {
        private const val FCM_FQN = "re.notifica.push.fcm.internal.PushServiceManager"
        private const val HMS_FQN = "re.notifica.push.hms.internal.PushServiceManager"

        internal fun create(): PushServiceManager {
            return Factory.create(
                gms = FCM_FQN,
                hms = HMS_FQN
            )
        }
    }
}
