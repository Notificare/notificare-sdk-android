package re.notifica.internal

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import re.notifica.Notificare
import re.notifica.internal.adapters.NotificareTimeAdapter
import re.notifica.internal.adapters.NotificareTransportAdapter
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

    val availableModules: List<String>
        get() {
            val modules = mutableListOf("core")

            if (Notificare.pushManager != null) modules.add("push")
            //if (Notificare.locationManager != null) modules.add("location")

            return modules
        }

    fun createMoshi(): Moshi {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .add(NotificareTimeAdapter())
            .add(NotificareTransportAdapter())
            .build()
    }
}
