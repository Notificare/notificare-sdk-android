package re.notifica.push.ui.hms.internal

import androidx.fragment.app.Fragment
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.hms.NotificareMapFragment
import re.notifica.push.ui.hms.NotificareRateFragment
import re.notifica.push.ui.hms.NotificareStoreFragment
import re.notifica.push.ui.internal.ServiceManager

@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override fun getFragmentClass(notification: NotificareNotification): Class<out Fragment> {
        return when (notification.type) {
            NotificareNotification.TYPE_MAP -> NotificareMapFragment::class.java
            NotificareNotification.TYPE_RATE -> NotificareRateFragment::class.java
            NotificareNotification.TYPE_STORE -> NotificareStoreFragment::class.java
            else -> throw IllegalArgumentException("Unhandled notification type '${notification.type}'.")
        }
    }
}
