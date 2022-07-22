package re.notifica.monetize.gms.internal.ktx

import com.android.billingclient.api.ProductDetails
import kotlin.math.round

internal val ProductDetails.OneTimePurchaseOfferDetails.priceAmount: Double
    get() {
        val cents = round(priceAmountMicros.toDouble() / 10000)
        return cents / 100
    }
