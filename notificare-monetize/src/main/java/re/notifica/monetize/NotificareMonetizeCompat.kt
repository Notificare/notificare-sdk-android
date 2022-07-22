package re.notifica.monetize

import android.app.Activity
import androidx.lifecycle.LiveData
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.monetize.ktx.monetize
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchase

public object NotificareMonetizeCompat {

    @JvmStatic
    public val products: List<NotificareProduct>
        get() = Notificare.monetize().products

    @JvmStatic
    public val observableProducts: LiveData<List<NotificareProduct>> =
        Notificare.monetize().observableProducts

    @JvmStatic
    public val purchases: List<NotificarePurchase>
        get() = Notificare.monetize().purchases

    @JvmStatic
    public val observablePurchases: LiveData<List<NotificarePurchase>> =
        Notificare.monetize().observablePurchases

    @JvmStatic
    public fun addListener(listener: NotificareMonetize.Listener) {
        Notificare.monetize().addListener(listener)
    }

    @JvmStatic
    public fun removeListener(listener: NotificareMonetize.Listener) {
        Notificare.monetize().removeListener(listener)
    }

    @JvmStatic
    public fun refresh(callback: NotificareCallback<Unit>) {
        Notificare.monetize().refresh(callback)
    }

    @JvmStatic
    public fun startPurchaseFlow(activity: Activity, product: NotificareProduct) {
        Notificare.monetize().startPurchaseFlow(activity, product)
    }
}
