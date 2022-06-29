package re.notifica.monetize.gms.internal.ktx

import com.android.billingclient.api.ProductDetails
import re.notifica.monetize.internal.network.push.FetchProductsResponse
import re.notifica.monetize.models.NotificareProduct

internal val FetchProductsResponse.Product.isGooglePlay: Boolean
    get() = stores.contains("GooglePlay")

internal fun FetchProductsResponse.Product.toModel(details: ProductDetails?): NotificareProduct {
    return NotificareProduct(
        id = _id,
        identifier = identifier,
        name = name,
        type = type,
        storeDetails = details?.let {
            NotificareProduct.StoreDetails(
                name = it.name,
                title = it.title,
                description = it.description,
                oneTimePurchaseOfferDetails = it.oneTimePurchaseOfferDetails?.let { purchaseDetails ->
                    NotificareProduct.StoreDetails.OneTimePurchaseOfferDetails(
                        formattedPrice = purchaseDetails.formattedPrice,
                        priceAmountMicros = purchaseDetails.priceAmountMicros,
                        priceCurrencyCode = purchaseDetails.priceCurrencyCode,
                    )
                },
            )
        }
    )
}
