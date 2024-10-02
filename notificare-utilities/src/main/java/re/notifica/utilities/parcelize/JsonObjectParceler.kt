package re.notifica.utilities.parcelize

import android.os.Parcel
import kotlinx.parcelize.Parceler
import org.json.JSONObject

public object JsonObjectParceler : Parceler<JSONObject?> {
    override fun create(parcel: Parcel): JSONObject? {
        if (parcel.readInt() == 0) {
            return null
        }

        val str = parcel.readString() ?: return null
        return JSONObject(str)
    }

    override fun JSONObject?.write(parcel: Parcel, flags: Int) {
        if (this == null) {
            parcel.writeInt(0)
        } else {
            parcel.writeInt(1)
            parcel.writeString(this.toString())
        }
    }
}
