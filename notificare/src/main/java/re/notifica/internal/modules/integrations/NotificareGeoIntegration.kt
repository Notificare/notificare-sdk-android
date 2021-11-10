package re.notifica.internal.modules.integrations

import android.location.Location
import kotlinx.coroutines.flow.StateFlow
import re.notifica.InternalNotificareApi

@InternalNotificareApi
public interface NotificareGeoIntegration {

    public val geoLastKnownLocation: Location?

    public val geoEnteredBeacons: List<Beacon>


    public data class Beacon(
        val major: Int,
        val minor: Int?,
    )
}
