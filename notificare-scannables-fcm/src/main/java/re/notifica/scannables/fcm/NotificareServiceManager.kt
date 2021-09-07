package re.notifica.scannables.fcm

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import re.notifica.scannables.NotificareServiceManager
import re.notifica.scannables.fcm.ui.QrCodeScannerFragment

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NotificareServiceManager(
    private val context: Context,
) : NotificareServiceManager {

    override val hasMobileServicesAvailable: Boolean
        get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    init {
        if (!hasMobileServicesAvailable) {
            throw IllegalStateException("Google Play Services are not available.")
        }
    }

    override fun getQrCodeScannerFragmentClass(): Class<out Fragment> {
        return QrCodeScannerFragment::class.java
    }
}
