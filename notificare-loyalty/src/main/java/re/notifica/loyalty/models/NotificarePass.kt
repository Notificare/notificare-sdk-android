package re.notifica.loyalty.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.internal.parcelize.NotificareExtraParceler
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificarePass(
    val id: String,
    val version: Int,
    val active: Boolean,
    val passbook: String?,
    val barcode: String,
    val serial: String,
    val redeem: Redeem,
    val limit: Int,
    val token: String,
    val data: @WriteWith<NotificareExtraParceler> Map<String, Any> = mapOf(),
    val date: Date,
    val redeemHistory: List<Redemption>,
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

    public enum class Redeem {
        @Json(name = "once")
        ONCE,

        @Json(name = "limit")
        LIMIT,

        @Json(name = "always")
        ALWAYS;
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Redemption(
        val comments: String?,
        val date: Date,
    ) : Parcelable
}
