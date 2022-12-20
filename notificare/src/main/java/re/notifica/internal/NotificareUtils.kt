package re.notifica.internal

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.suspendCancellableCoroutine
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.internal.ktx.applicationInfo
import re.notifica.internal.ktx.packageInfo
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@InternalNotificareApi
public object NotificareUtils {
    public val applicationName: String
        get() {
            return try {
                val context = Notificare.requireContext().applicationContext
                context.packageManager.getApplicationLabel(
                    context.packageManager.applicationInfo(context.packageName, 0)
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
                context.packageManager.packageInfo(context.packageName, 0).versionName
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

    public val userAgent: String
        get() = "$applicationName/$applicationVersion Notificare/${Notificare.SDK_VERSION} Android/${Build.VERSION.RELEASE}"

    internal fun getEnabledPeerModules(): List<String> {
        return NotificareModule.Module.values()
            .filter { it.isPeer && it.isAvailable }
            .map { it.name.lowercase() }
    }

    public suspend fun loadBitmap(url: String): Bitmap = suspendCancellableCoroutine { continuation ->
        Glide.with(Notificare.requireContext())
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    continuation.resume(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    continuation.resumeWithException(RuntimeException("Failed to load the bit at $url"))
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    public fun loadImage(url: String, view: ImageView) {
        Glide.with(Notificare.requireContext())
            .load(url)
            .into(view)
    }
}
