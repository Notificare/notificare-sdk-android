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
        val passbook: String?,
        val template: String?,
        val barcode: String,
        val serial: String,
        val redeem: NotificarePass.Redeem,
        val redeemHistory: List<NotificarePass.Redemption>,
        val limit: Int,
        val token: String,
        val data: Map<String, Any>?,
        val date: Date,
        val lastUpdated: Date?,
    )
}

@JsonClass(generateAdapter = true)
internal data class FetchPassbookTemplateResponse(
    val passbook: Passbook,
) {

    @UseDefaultsWhenNull
    @JsonClass(generateAdapter = true)
    internal data class Passbook(
        val passStyle: NotificarePass.PassType,
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

@JsonClass(generateAdapter = true)
internal data class FetchUpdatedSerialsResponse(
    val serialNumbers: List<String>,
    val lastUpdated: String,
)
