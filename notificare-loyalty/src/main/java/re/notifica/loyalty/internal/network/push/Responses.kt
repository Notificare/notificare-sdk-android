package re.notifica.loyalty.internal.network.push

import com.squareup.moshi.JsonClass
import re.notifica.internal.moshi.UseDefaultsWhenNull
import re.notifica.loyalty.models.NotificarePass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class FetchPassResponse(
    val pass: Pass,
) {
    @UseDefaultsWhenNull
    @JsonClass(generateAdapter = true)
    internal data class Pass(
        val _id: String,
        val version: Int,
        val active: Boolean,
        val passbook: String?,
        val barcode: String,
        val serial: String,
        val redeem: NotificarePass.Redeem,
        val limit: Int,
        val token: String,
        val data: Map<String, Any>?,
        val date: Date,
        val redeemHistory: List<NotificarePass.Redemption>,
    )
}

@JsonClass(generateAdapter = true)
internal data class FetchSaveLinksResponse(
    val saveLinks: SaveLinks?,
) {

    @JsonClass(generateAdapter = true)
    internal data class SaveLinks(
        val googlePay: String?,
    )
}

internal fun FetchPassResponse.Pass.toModel(): NotificarePass {
    return NotificarePass(
        id = _id,
        version = version,
        active = active,
        passbook = passbook,
        barcode = barcode,
        serial = serial,
        redeem = redeem,
        limit = limit,
        token = token,
        data = data ?: mapOf(),
        date = date,
        redeemHistory = redeemHistory,
    )
}
