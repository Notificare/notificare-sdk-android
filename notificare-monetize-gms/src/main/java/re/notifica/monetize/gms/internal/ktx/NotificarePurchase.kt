package re.notifica.monetize.gms.internal.ktx

import com.android.billingclient.api.Purchase
import re.notifica.monetize.models.NotificarePurchase
import java.util.*

internal fun NotificarePurchase.Companion.from(purchase: Purchase): NotificarePurchase {
    return NotificarePurchase(
        id = purchase.orderId,
        productIdentifier = purchase.products.first(),
        originalJson = purchase.originalJson,
        packageName = purchase.packageName,
        time = Date(purchase.purchaseTime),
        token = purchase.purchaseToken,
        signature = purchase.signature,
        isAcknowledged = purchase.isAcknowledged,
    )
}
