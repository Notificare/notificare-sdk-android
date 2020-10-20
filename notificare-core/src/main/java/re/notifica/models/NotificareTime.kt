package re.notifica.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificareTime(
    val hours: Int,
    val minutes: Int
) {

    init {
        if (hours !in 0..23 || minutes !in 0..59) {
            throw IllegalArgumentException("Invalid time.")
        }
    }

    constructor(timeStr: String) : this(parse(timeStr).first, parse(timeStr).second)

    fun format(): String = "%02d:%02d".format(hours, minutes)

    companion object {
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
