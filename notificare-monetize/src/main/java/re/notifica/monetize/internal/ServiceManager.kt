package re.notifica.monetize.internal

import android.app.Activity
import androidx.lifecycle.LiveData
import re.notifica.InternalNotificareApi
import re.notifica.internal.AbstractServiceManager
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchase

private typealias OnBillingSetupFinished = () -> Unit
private typealias OnBillingSetupFailed = (code: Int, message: String) -> Unit
private typealias OnProductsUpdated = (products: List<NotificareProduct>) -> Unit
private typealias OnPurchaseFinished = (purchase: NotificarePurchase) -> Unit
private typealias OnPurchaseCanceled = () -> Unit
private typealias OnPurchaseFailed = (code: Int, message: String) -> Unit

@InternalNotificareApi
public abstract class ServiceManager : AbstractServiceManager() {

    public lateinit var onBillingSetupFinished: OnBillingSetupFinished
    public lateinit var onBillingSetupFailed: OnBillingSetupFailed
    public lateinit var onProductsUpdated: OnProductsUpdated
    public lateinit var onPurchaseFinished: OnPurchaseFinished
    public lateinit var onPurchaseCanceled: OnPurchaseCanceled
    public lateinit var onPurchaseFailed: OnPurchaseFailed

    public abstract fun startConnection()

    public abstract fun stopConnection()

    public abstract suspend fun refresh()

    public abstract fun startPurchaseFlow(activity: Activity, product: NotificareProduct)

    public abstract suspend fun fetchPurchases(): List<NotificarePurchase>

    internal companion object {
        private const val GMS_FQN = "re.notifica.monetize.gms.internal.ServiceManager"
        private const val HMS_FQN = "re.notifica.monetize.hms.internal.ServiceManager"

        internal fun create(
            onBillingSetupFinished: OnBillingSetupFinished,
            onBillingSetupFailed: OnBillingSetupFailed,
            onProductsUpdated: OnProductsUpdated,
            onPurchaseFinished: OnPurchaseFinished,
            onPurchaseCanceled: OnPurchaseCanceled,
            onPurchaseFailed: OnPurchaseFailed,
        ): ServiceManager {
            return Factory.create<ServiceManager>(
                gms = GMS_FQN,
                hms = HMS_FQN
            ).apply {
                this.onBillingSetupFinished = onBillingSetupFinished
                this.onBillingSetupFailed = onBillingSetupFailed
                this.onProductsUpdated = onProductsUpdated
                this.onPurchaseFinished = onPurchaseFinished
                this.onPurchaseCanceled = onPurchaseCanceled
                this.onPurchaseFailed = onPurchaseFailed
            }
        }
    }
}
