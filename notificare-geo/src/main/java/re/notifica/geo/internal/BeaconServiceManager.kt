package re.notifica.geo.internal

import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareRegion
import re.notifica.internal.NotificareLogger

@InternalNotificareApi
public abstract class BeaconServiceManager(
    protected val proximityUUID: String,
) {

    public abstract fun startMonitoring(region: NotificareRegion, beacons: List<NotificareBeacon>)

    public abstract fun stopMonitoring(region: NotificareRegion)

    public abstract fun clearMonitoring()

    public companion object {
        private const val FQN = "re.notifica.geo.beacons.internal.BeaconServiceManager"

        internal fun create(): BeaconServiceManager? {
            val proximityUUID = Notificare.application?.regionConfig?.proximityUUID ?: run {
                NotificareLogger.warning("The Proximity UUID property has not been configured for this application.")
                return null
            }

            return try {
                val klass = Class.forName(FQN)
                klass.getConstructor(String::class.java).newInstance(proximityUUID) as? BeaconServiceManager
            } catch (e: Exception) {
                null
            }
        }
    }


    public data class Beacon(
        val major: Int,
        val minor: Int,
        val proximity: Double?,
    )
}
