package re.notifica.scannables.gms.internal

import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.scannables.gms.ui.QrCodeScannerFragment
import re.notifica.scannables.internal.ServiceManager

@Keep
@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override fun getQrCodeScannerFragmentClass(): Class<out Fragment> {
        return QrCodeScannerFragment::class.java
    }
}
