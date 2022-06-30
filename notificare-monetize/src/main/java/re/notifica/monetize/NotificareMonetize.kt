package re.notifica.monetize

import android.app.Activity
import androidx.lifecycle.LiveData
import re.notifica.InternalNotificareApi
import re.notifica.NotificareCallback
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchaseVerification

public interface NotificareMonetize {

    public val products: List<NotificareProduct>

    public val observableProducts: LiveData<List<NotificareProduct>>

    public suspend fun refresh()

    public fun refresh(callback: NotificareCallback<Unit>)

    public fun startPurchaseFlow(activity: Activity, product: NotificareProduct)
}

@InternalNotificareApi
public interface NotificareInternalMonetize {

    @InternalNotificareApi
    public suspend fun verifyPurchase(purchase: NotificarePurchaseVerification)
}
