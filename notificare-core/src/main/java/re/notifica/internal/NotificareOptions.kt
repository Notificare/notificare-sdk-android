package re.notifica.internal

import android.content.Context
import android.content.pm.PackageManager

class NotificareOptions(context: Context) {

    val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

    val crashReportsEnabled: Boolean
        get() {
            return info.metaData.getBoolean("re.notifica.crash_reports_enabled", true)
        }
}
