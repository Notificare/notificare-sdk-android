package re.notifica.loyalty.internal.network.push

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RegisterPassPayload(
    val transport: String,
)
