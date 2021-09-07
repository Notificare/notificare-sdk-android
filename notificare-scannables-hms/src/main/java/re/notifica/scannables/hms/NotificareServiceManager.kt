package re.notifica.scannables.hms

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import re.notifica.scannables.NotificareServiceManager
import re.notifica.scannables.hms.ui.QrCodeScannerFragment

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NotificareServiceManager(
    private val context: Context,
) : NotificareServiceManager {

    override val hasMobileServicesAvailable: Boolean
        get() = HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) == ConnectionResult.SUCCESS

    init {
        if (!hasMobileServicesAvailable) {
            throw IllegalStateException("Huawei Mobile Services are not available.")
        }
    }

    override fun getQrCodeScannerFragmentClass(): Class<out Fragment> {
        return QrCodeScannerFragment::class.java
    }
}
