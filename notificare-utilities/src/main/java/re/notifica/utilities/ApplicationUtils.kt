package re.notifica.utilities

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

public fun getApplicationName(context: Context): String {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(context.packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        Log.e("AppInfo", "Application name not found.", e)
        "unknown"
    }
}

public fun getApplicationVersion(context: Context): String {
    return try {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e("AppInfo", "Application version not found.", e)
        "unknown"
    }
}

public fun getUserAgent(context: Context, sdk: String): String {
    val applicationName = getApplicationName(context)
    val applicationVersion = getApplicationVersion(context)
    return "$applicationName/$applicationVersion Notificare/$sdk Android/${Build.VERSION.RELEASE}"
}
