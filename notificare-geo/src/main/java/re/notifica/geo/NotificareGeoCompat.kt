package re.notifica.geo

import re.notifica.Notificare
import re.notifica.geo.ktx.INTENT_ACTION_BEACONS_RANGED
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_ENTERED
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_EXITED
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_NOTIFICATION_OPENED
import re.notifica.geo.ktx.INTENT_ACTION_LOCATION_UPDATED
import re.notifica.geo.ktx.INTENT_ACTION_REGION_ENTERED
import re.notifica.geo.ktx.INTENT_ACTION_REGION_EXITED
import re.notifica.geo.ktx.INTENT_EXTRA_BEACON
import re.notifica.geo.ktx.INTENT_EXTRA_LOCATION
import re.notifica.geo.ktx.INTENT_EXTRA_RANGED_BEACONS
import re.notifica.geo.ktx.INTENT_EXTRA_REGION
import re.notifica.geo.ktx.geo
import re.notifica.geo.models.NotificareRegion

public object NotificareGeoCompat {

    // region Intent actions

    @JvmField
    public val INTENT_ACTION_LOCATION_UPDATED: String = Notificare.INTENT_ACTION_LOCATION_UPDATED

    @JvmField
    public val INTENT_ACTION_REGION_ENTERED: String = Notificare.INTENT_ACTION_REGION_ENTERED

    @JvmField
    public val INTENT_ACTION_REGION_EXITED: String = Notificare.INTENT_ACTION_REGION_EXITED

    @JvmField
    public val INTENT_ACTION_BEACON_ENTERED: String = Notificare.INTENT_ACTION_BEACON_ENTERED

    @JvmField
    public val INTENT_ACTION_BEACON_EXITED: String = Notificare.INTENT_ACTION_BEACON_EXITED

    @JvmField
    public val INTENT_ACTION_BEACONS_RANGED: String = Notificare.INTENT_ACTION_BEACONS_RANGED

    @JvmField
    public val INTENT_ACTION_BEACON_NOTIFICATION_OPENED: String = Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED

    // endregion

    // region Intent extras

    @JvmField
    public val INTENT_EXTRA_LOCATION: String = Notificare.INTENT_EXTRA_LOCATION

    @JvmField
    public val INTENT_EXTRA_REGION: String = Notificare.INTENT_EXTRA_REGION

    @JvmField
    public val INTENT_EXTRA_BEACON: String = Notificare.INTENT_EXTRA_BEACON

    @JvmField
    public val INTENT_EXTRA_RANGED_BEACONS: String = Notificare.INTENT_EXTRA_RANGED_BEACONS

// endregion

    @JvmStatic
    public var intentReceiver: Class<out NotificareGeoIntentReceiver>
        get() = Notificare.geo().intentReceiver
        set(value) {
            Notificare.geo().intentReceiver = value
        }

    @JvmStatic
    public val hasLocationServicesEnabled: Boolean
        get() = Notificare.geo().hasLocationServicesEnabled

    @JvmStatic
    public val hasBluetoothEnabled: Boolean
        get() = Notificare.geo().hasBluetoothEnabled

    @JvmStatic
    public val monitoredRegions: List<NotificareRegion>
        get() = Notificare.geo().monitoredRegions

    @JvmStatic
    public fun enableLocationUpdates() {
        Notificare.geo().enableLocationUpdates()
    }

    @JvmStatic
    public fun disableLocationUpdates() {
        Notificare.geo().disableLocationUpdates()
    }

    @JvmStatic
    public fun addListener(listener: NotificareGeo.Listener) {
        Notificare.geo().addListener(listener)
    }

    @JvmStatic
    public fun removeListener(listener: NotificareGeo.Listener) {
        Notificare.geo().removeListener(listener)
    }
}
