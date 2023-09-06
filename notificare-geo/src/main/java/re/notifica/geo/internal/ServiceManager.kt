package re.notifica.geo.internal

import android.location.Location
import kotlinx.coroutines.Deferred
import re.notifica.InternalNotificareApi
import re.notifica.geo.models.NotificareRegion
import re.notifica.internal.AbstractServiceManager

@InternalNotificareApi
public abstract class ServiceManager : AbstractServiceManager() {

    public abstract fun enableLocationUpdates()

    public abstract fun disableLocationUpdates()

    public abstract fun getCurrentLocationAsync(): Deferred<Location>

    public abstract suspend fun startMonitoringRegions(regions: List<NotificareRegion>)

    public abstract suspend fun stopMonitoringRegions(regions: List<NotificareRegion>)

    public abstract suspend fun clearMonitoringRegions()

    internal companion object {
        private const val GMS_FQN = "re.notifica.geo.gms.internal.ServiceManager"
        private const val HMS_FQN = "re.notifica.geo.hms.internal.ServiceManager"

        internal fun create(): ServiceManager {
            return Factory.create(
                gms = GMS_FQN,
                hms = HMS_FQN
            )
        }
    }
}
