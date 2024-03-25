package re.notifica.internal.ktx

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import re.notifica.InternalNotificareApi

@InternalNotificareApi
public fun PackageManager.activityInfo(component: ComponentName, flags: Int): ActivityInfo {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            getActivityInfo(component, PackageManager.ComponentInfoFlags.of(flags.toLong()))
        }
        else ->
            @Suppress("DEPRECATION")
            getActivityInfo(component, flags)
    }
}

@InternalNotificareApi
public fun PackageManager.applicationInfo(packageName: String, flags: Int): ApplicationInfo {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        }
        else ->
            @Suppress("DEPRECATION")
            getApplicationInfo(packageName, flags)
    }
}

@InternalNotificareApi
public fun PackageManager.packageInfo(packageName: String, flags: Int): PackageInfo {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        }
        else ->
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, flags)
    }
}
