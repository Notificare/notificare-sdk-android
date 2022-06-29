package re.notifica.monetize

import android.app.Activity
import androidx.lifecycle.LiveData
import re.notifica.NotificareCallback
import re.notifica.monetize.models.NotificareProduct

public interface NotificareMonetize {

    public val products: List<NotificareProduct>

    public val observableProducts: LiveData<List<NotificareProduct>>

    public suspend fun refresh()

    public fun refresh(callback: NotificareCallback<Unit>)

    public fun startPurchaseFlow(activity: Activity, product: NotificareProduct)
}
