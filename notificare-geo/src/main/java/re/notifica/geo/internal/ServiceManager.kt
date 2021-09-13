package re.notifica.geo.internal

import re.notifica.InternalNotificareApi
import re.notifica.internal.AbstractServiceManager

@InternalNotificareApi
public abstract class ServiceManager : AbstractServiceManager() {

    public abstract fun enableLocationUpdates()

    internal companion object {
        private const val FCM_FQN = "re.notifica.geo.fcm.internal.ServiceManager"
        private const val HMS_FQN = "re.notifica.geo.hms.internal.ServiceManager"

        internal fun create(): ServiceManager {
            return Factory.create(
                gms = FCM_FQN,
                hms = HMS_FQN
            )
        }
    }
}
