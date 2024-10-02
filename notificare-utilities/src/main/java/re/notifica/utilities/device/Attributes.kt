package re.notifica.utilities.device

import android.os.Build
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

public val deviceString: String
    get() = "${Build.MANUFACTURER} ${Build.MODEL}"

public val deviceLanguage: String
    get() = Locale.getDefault().language

public val deviceRegion: String
    get() = Locale.getDefault().country

public val osVersion: String
    get() = Build.VERSION.RELEASE

public val timeZoneOffset: Double
    get() {
        val timeZone = TimeZone.getDefault()
        val calendar = GregorianCalendar.getInstance(timeZone)

        return timeZone.getOffset(calendar.timeInMillis) / 3600000.toDouble()
    }
