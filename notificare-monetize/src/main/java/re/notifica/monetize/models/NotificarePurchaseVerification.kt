package re.notifica.monetize.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
public data class NotificarePurchaseVerification(
    val receipt: String,
    val signature: String,
    val price: Double,
    val currency: String,
)
