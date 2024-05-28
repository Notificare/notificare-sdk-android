package re.notifica.loyalty.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.internal.parcelize.NotificareExtraParceler

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificarePass(
    val id: String,
    val type: PassType?,
    val version: Int,
    val passbook: String?,
    val template: String?,
    val serial: String,
    val barcode: String,
    val redeem: Redeem,
    val redeemHistory: List<Redemption>,
    val limit: Int,
    val token: String,
    val data: @WriteWith<NotificareExtraParceler> Map<String, Any> = mapOf(),
    val date: Date,
    val googlePaySaveLink: String?,
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificarePass::class.java)

        public fun fromJson(json: JSONObject): NotificarePass {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = false)
    public enum class Redeem : Parcelable {
        @Json(name = "once")
        ONCE,

        @Json(name = "limit")
        LIMIT,

        @Json(name = "always")
        ALWAYS;
    }

    @Parcelize
    @JsonClass(generateAdapter = false)
    public enum class PassType : Parcelable {
        @Json(name = "boarding")
        BOARDING,

        @Json(name = "coupon")
        COUPON,

        @Json(name = "ticket")
        TICKET,

        @Json(name = "generic")
        GENERIC,

        @Json(name = "card")
        CARD;
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Redemption(
        val comments: String?,
        val date: Date,
    ) : Parcelable
}
