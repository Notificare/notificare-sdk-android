package re.notifica.monetize.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificarePurchase(
    val orderId: String,
    val productIdentifier: String,
    val originalJson: String,
    val packageName: String?,
    val time: Date,
    val token: String,
    val signature: String,
    val isAcknowledged: Boolean,
) : Parcelable {

    public companion object
}
