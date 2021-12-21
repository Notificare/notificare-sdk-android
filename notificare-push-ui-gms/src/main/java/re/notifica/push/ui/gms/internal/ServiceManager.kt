package re.notifica.push.ui.gms.internal

import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.gms.NotificareMapFragment
import re.notifica.push.ui.gms.NotificareRateFragment
import re.notifica.push.ui.gms.NotificareStoreFragment
import re.notifica.push.ui.internal.ServiceManager

@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override fun getFragmentClass(notification: NotificareNotification): Class<out Fragment> {
        return when (notification.type) {
            NotificareNotification.TYPE_MAP -> NotificareMapFragment::class.java
            NotificareNotification.TYPE_RATE -> NotificareRateFragment::class.java
            NotificareNotification.TYPE_STORE -> NotificareStoreFragment::class.java
            else -> throw IllegalArgumentException("Unhandled notification type '${notification.type}'.")
        }
    }
}
