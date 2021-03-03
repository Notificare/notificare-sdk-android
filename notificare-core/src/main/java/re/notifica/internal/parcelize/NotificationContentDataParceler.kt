package re.notifica.internal.parcelize

import android.os.Parcel
import kotlinx.parcelize.Parceler

internal object NotificationContentDataParceler : Parceler<Any> {
    override fun create(parcel: Parcel): Any {
        val typeStr = parcel.readString()
            ?: throw IllegalArgumentException("Missing type string in parcel.")

        return when (typeStr) {
            "string" -> checkNotNull(parcel.readString())
            "map" -> mutableMapOf<String, String>().apply {
                parcel.readMap(this, String::class.java.classLoader)
            }
            else -> throw IllegalArgumentException("Unexpected type string '$typeStr'.")
        }
    }

    override fun Any.write(parcel: Parcel, flags: Int) {
        when (this) {
            is String -> {
                parcel.writeString("string")
                parcel.writeString(this)
            }
            is Map<*, *> -> {
                parcel.writeString("map")
                parcel.writeMap(this)
            }
            else -> throw IllegalArgumentException("Cannot parcelize type '${this::class.java}'.")
        }
    }
}
