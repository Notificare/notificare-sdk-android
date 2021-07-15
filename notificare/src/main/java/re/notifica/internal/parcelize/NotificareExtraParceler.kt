package re.notifica.internal.parcelize

import android.os.Parcel
import androidx.annotation.RestrictTo
import com.squareup.moshi.Types
import kotlinx.parcelize.Parceler
import re.notifica.Notificare

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object NotificareExtraParceler : Parceler<Map<String, Any>> {
    override fun create(parcel: Parcel): Map<String, Any> {
        val str = parcel.readString() ?: return mapOf()

        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = Notificare.moshi.adapter<Map<String, Any>>(type)

        return adapter.fromJson(str) ?: mapOf()
    }

    override fun Map<String, Any>.write(parcel: Parcel, flags: Int) {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = Notificare.moshi.adapter<Map<String, Any>>(type)

        val str = adapter.toJson(this)
        parcel.writeString(str)
    }
}
