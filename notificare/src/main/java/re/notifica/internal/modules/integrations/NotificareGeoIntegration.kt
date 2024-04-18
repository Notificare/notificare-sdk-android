package re.notifica.internal.modules.integrations

import android.location.Location
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
