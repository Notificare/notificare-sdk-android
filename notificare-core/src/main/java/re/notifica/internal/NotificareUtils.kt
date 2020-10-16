package re.notifica.internal

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import re.notifica.Notificare
import java.util.*

internal object NotificareUtils {
    val applicationVersion: String
        get() {
            return try {
                val context = Notificare.requireContext().applicationContext
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("Notificare", "Application version not found.", e)
                "unknown"
            }
        }

    val deviceString: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"

    val deviceLanguage: String
        get() = Locale.getDefault().language

    val deviceRegion: String
        get() = Locale.getDefault().country

    val osVersion: String
        get() = Build.VERSION.RELEASE

    val timeZoneOffset: Float
        get() {
            val timeZone = TimeZone.getDefault()
            val calendar = GregorianCalendar.getInstance(timeZone)

            return timeZone.getOffset(calendar.timeInMillis) / 3600000f
        }
}
