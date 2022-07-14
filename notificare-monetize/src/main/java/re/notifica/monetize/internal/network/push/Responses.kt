package re.notifica.monetize.internal.network.push

import com.squareup.moshi.JsonClass
import re.notifica.InternalNotificareApi

@InternalNotificareApi
@JsonClass(generateAdapter = true)
public data class FetchProductsResponse(
    val products: List<Product>,
) {

    @InternalNotificareApi
    @JsonClass(generateAdapter = true)
    public data class Product(
        val _id: String,
        val identifier: String,
        val name: String,
        val type: String,
        val stores: List<String>,
    )
}
