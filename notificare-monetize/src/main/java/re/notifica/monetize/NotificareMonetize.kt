package re.notifica.monetize

import android.app.Activity
import androidx.lifecycle.LiveData
import re.notifica.InternalNotificareApi
import re.notifica.NotificareCallback
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchase
import re.notifica.monetize.models.NotificarePurchaseVerification

public interface NotificareMonetize {

    public val products: List<NotificareProduct>

    public val observableProducts: LiveData<List<NotificareProduct>>

    public val purchases: List<NotificarePurchase>

    public val observablePurchases: LiveData<List<NotificarePurchase>>

    public fun addListener(listener: Listener)

    public fun removeListener(listener: Listener)

    public suspend fun refresh()

    public fun refresh(callback: NotificareCallback<Unit>)

    public fun startPurchaseFlow(activity: Activity, product: NotificareProduct)

    public interface Listener {
        public fun onBillingSetupFinished() {}

        public fun onBillingSetupFailed(code: Int, message: String) {}

        public fun onPurchaseFinished(purchase: NotificarePurchase) {}

        public fun onPurchaseRestored(purchase: NotificarePurchase) {}

        public fun onPurchaseCanceled() {}

        public fun onPurchaseFailed(code: Int, message: String) {}
    }
}

@InternalNotificareApi
public interface NotificareInternalMonetize {

    @InternalNotificareApi
    public suspend fun verifyPurchase(purchase: NotificarePurchaseVerification)
}
