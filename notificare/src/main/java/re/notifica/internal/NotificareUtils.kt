package re.notifica.internal

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import java.util.*

@InternalNotificareApi
public object NotificareUtils {
    public val applicationName: String
        get() {
            return try {
                val context = Notificare.requireContext().applicationContext
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(context.packageName, 0)
                ).toString()
            } catch (e: Exception) {
                Log.e("Notificare", "Application name not found.", e)
                return "unknown"
            }
        }

    public val applicationVersion: String
        get() {
            return try {
                val context = Notificare.requireContext().applicationContext
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("Notificare", "Application version not found.", e)
                "unknown"
            }
        }

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

    internal fun getEnabledPeerModules(): List<String> {
        return NotificareModule.Module.values()
            .filter { it.isPeer && it.isAvailable }
            .map { it.name.lowercase() }
    }

    public suspend fun loadBitmap(url: String): Bitmap = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        val bitmap = Glide.with(Notificare.requireContext())
            .asBitmap()
            .load(url)
            .submit()
            .get()

        withContext(Dispatchers.Main) { bitmap }
    }

    public fun loadImage(url: String, view: ImageView) {
        Glide.with(Notificare.requireContext())
            .load(url)
            .into(view)
    }
}
