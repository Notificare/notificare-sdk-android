package re.notifica.scannables.internal

import androidx.fragment.app.Fragment
import re.notifica.InternalNotificareApi
import re.notifica.internal.AbstractServiceManager

@InternalNotificareApi
public abstract class ServiceManager : AbstractServiceManager() {

    public abstract fun getQrCodeScannerFragmentClass(): Class<out Fragment>

    internal companion object {
        private const val GMS_FQN = "re.notifica.scannables.gms.internal.ServiceManager"
        private const val HMS_FQN = "re.notifica.scannables.hms.internal.ServiceManager"

        internal fun create(): ServiceManager {
            return Factory.create(
                gms = GMS_FQN,
                hms = HMS_FQN
            )
        }
    }
}
