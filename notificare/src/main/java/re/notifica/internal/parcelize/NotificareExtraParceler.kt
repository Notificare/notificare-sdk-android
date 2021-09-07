package re.notifica.internal.parcelize

import android.os.Parcel
import com.squareup.moshi.Types
import kotlinx.parcelize.Parceler
import re.notifica.InternalNotificareApi
import re.notifica.Notificare

@InternalNotificareApi
public object NotificareExtraParceler : Parceler<Map<String, Any>> {
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
