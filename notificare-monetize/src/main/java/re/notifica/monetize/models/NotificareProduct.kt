package re.notifica.monetize.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareProduct(
    val id: String,
    val identifier: String,
    val name: String,
    val type: String,
    val storeDetails: StoreDetails?,
) : Parcelable {

    public companion object {
        public const val TYPE_CONSUMABLE: String = "consumable"
        public const val TYPE_ONE_TIME: String = "onetime"
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class StoreDetails(
        val name: String,
        val title: String,
        val description: String,
        val oneTimePurchaseOfferDetails: OneTimePurchaseOfferDetails?,
        // val subscriptionOfferDetails: SubscriptionOfferDetails?,
    ) : Parcelable {

        @Parcelize
        @JsonClass(generateAdapter = true)
        public data class OneTimePurchaseOfferDetails(
            val formattedPrice: String,
            val priceAmountMicros: Long,
            val priceCurrencyCode: String,
        ) : Parcelable
    }
}
