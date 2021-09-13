package re.notifica.geo.fcm.internal

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.geo.internal.ServiceManager

@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override fun enableLocationUpdates() {
        TODO("Not yet implemented")
    }
}
