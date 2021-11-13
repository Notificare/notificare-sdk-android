package re.notifica.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareTime(
    val hours: Int,
    val minutes: Int
) : Parcelable {

    init {
        if (hours !in 0..23 || minutes !in 0..59) {
            throw IllegalArgumentException("Invalid time '$hours:$minutes'.")
        }
    }

    public constructor(timeStr: String) : this(parse(timeStr).first, parse(timeStr).second)

    public fun format(): String = "%02d:%02d".format(hours, minutes)

    private companion object {
        private fun parse(timeStr: String): Pair<Int, Int> {
            val parts = timeStr.split(":")
            if (parts.size != 2) throw IllegalArgumentException("Invalid time string.")

            val hours = parts[0].toIntOrNull()
            val minutes = parts[1].toIntOrNull()

            if (hours == null || minutes == null) throw IllegalArgumentException("Invalid time string.")

            return Pair(hours, minutes)
        }
    }
}
