package re.notifica.push.internal

import re.notifica.InternalNotificareApi
import re.notifica.internal.AbstractServiceManager
import re.notifica.push.models.NotificareTransport

@InternalNotificareApi
public abstract class ServiceManager : AbstractServiceManager() {

    public abstract val transport: NotificareTransport

    public abstract suspend fun getPushToken(): String

    internal companion object {
        private const val GMS_FQN = "re.notifica.push.gms.internal.ServiceManager"
        private const val HMS_FQN = "re.notifica.push.hms.internal.ServiceManager"

        internal fun create(): ServiceManager {
            return Factory.create(
                gms = GMS_FQN,
                hms = HMS_FQN
            )
        }
    }
}
