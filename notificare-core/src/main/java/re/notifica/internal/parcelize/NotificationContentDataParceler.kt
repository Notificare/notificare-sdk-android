package re.notifica.internal.parcelize

import android.os.Parcel
import kotlinx.parcelize.Parceler

internal object NotificationContentDataParceler : Parceler<Any> {
    override fun create(parcel: Parcel): Any {
        val str = parcel.readString()
        if (str != null) return str

        val map = mapOf<String, String>()
        parcel.readMap(map, String::class.java.classLoader)

        return map
    }

    override fun Any.write(parcel: Parcel, flags: Int) {
        when (this) {
            is String -> parcel.writeString(this)
            is Map<*, *> -> parcel.writeMap(this)
            else -> throw IllegalArgumentException("Cannot parcelize type '${this::class.java}'.")
        }
    }
}
