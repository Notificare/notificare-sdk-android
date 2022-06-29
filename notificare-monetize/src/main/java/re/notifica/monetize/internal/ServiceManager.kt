package re.notifica.monetize.internal

import android.app.Activity
import androidx.lifecycle.LiveData
import re.notifica.InternalNotificareApi
import re.notifica.internal.AbstractServiceManager
import re.notifica.monetize.models.NotificareProduct

@InternalNotificareApi
public abstract class ServiceManager : AbstractServiceManager() {

    public abstract val observableProducts: LiveData<List<NotificareProduct>>

    public abstract fun startConnection()

    public abstract fun stopConnection()

    public abstract suspend fun refresh()

    public abstract fun startPurchaseFlow(activity: Activity, product: NotificareProduct)

    internal companion object {
        private const val GMS_FQN = "re.notifica.monetize.gms.internal.ServiceManager"
        private const val HMS_FQN = "re.notifica.monetize.hms.internal.ServiceManager"

        internal fun create(): ServiceManager {
            return Factory.create(
                gms = GMS_FQN,
                hms = HMS_FQN
            )
        }
    }
}
