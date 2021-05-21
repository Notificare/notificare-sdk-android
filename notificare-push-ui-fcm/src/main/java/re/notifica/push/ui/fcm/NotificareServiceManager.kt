package re.notifica.push.ui.fcm

import android.content.Context
import androidx.annotation.RestrictTo
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificareServiceManager

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NotificareServiceManager(
    private val context: Context,
) : NotificareServiceManager {

    override val hasMobileServicesAvailable: Boolean
        get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    override fun getFragmentCanonicalClassName(notification: NotificareNotification): String? {
        return when (notification.type) {
            NotificareNotification.TYPE_MAP -> NotificareMapFragment::class.java.canonicalName
            NotificareNotification.TYPE_RATE -> NotificareRateFragment::class.java.canonicalName
            NotificareNotification.TYPE_STORE -> NotificareStoreFragment::class.java.canonicalName
            else -> throw IllegalArgumentException("Unhandled notification type '${notification.type}'.")
        }
    }
}