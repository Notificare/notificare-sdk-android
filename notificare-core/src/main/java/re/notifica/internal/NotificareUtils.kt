package re.notifica.internal

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.internal.adapters.NotificareTimeAdapter
import re.notifica.internal.adapters.NotificareTransportAdapter
import java.util.*

object NotificareUtils {
    val applicationName: String
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

    fun getLoadedModules(): List<String> {
        val modules = mutableListOf<String>()

        NotificareDefinitions.Module.values().forEach { module ->
            if (module.isAvailable) {
                modules.add(module.name.toLowerCase(Locale.ROOT))
            }
        }

        return modules
    }

    fun createMoshi(): Moshi {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .add(NotificareTimeAdapter())
            .add(NotificareTransportAdapter())
            .build()
    }

    suspend fun loadBitmap(url: String): Bitmap {
        return withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            val bitmap = Glide.with(Notificare.requireContext())
                .asBitmap()
                .load(url)
                .submit()
                .get()

            withContext(Dispatchers.Main) { bitmap }
        }
    }

    fun loadImage(url: String, view: ImageView) {
        Glide.with(Notificare.requireContext())
            .load(url)
            .into(view)
    }
}
