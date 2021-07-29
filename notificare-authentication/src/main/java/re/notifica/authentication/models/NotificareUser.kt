package re.notifica.authentication.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareUser(
    val id: String,
    val name: String,
    val pushEmailAddress: String?,
    val segments: List<String>,
    val registrationDate: Date,
    val lastActive: Date,
) : Parcelable {

    fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    companion object {
        private val adapter = Notificare.moshi.adapter(NotificareUser::class.java)

        fun fromJson(json: JSONObject): NotificareUser {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
