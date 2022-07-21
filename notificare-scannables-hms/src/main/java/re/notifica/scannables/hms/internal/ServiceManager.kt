package re.notifica.scannables.hms.internal

import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.scannables.hms.ui.QrCodeScannerFragment
import re.notifica.scannables.internal.ServiceManager

@Keep
@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override fun getQrCodeScannerFragmentClass(): Class<out Fragment> {
        return QrCodeScannerFragment::class.java
    }
}
