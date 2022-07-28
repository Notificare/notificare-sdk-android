package re.notifica.geo

import re.notifica.Notificare
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_NOTIFICATION_OPENED
import re.notifica.geo.ktx.geo

public object NotificareGeoCompat {

    // region Intent actions

    @JvmField
    public val INTENT_ACTION_BEACON_NOTIFICATION_OPENED: String = Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED

    // endregion

    @JvmStatic
    public val hasLocationServicesEnabled: Boolean
        get() = Notificare.geo().hasLocationServicesEnabled

    @JvmStatic
    public val hasBluetoothEnabled: Boolean
        get() = Notificare.geo().hasBluetoothEnabled

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
