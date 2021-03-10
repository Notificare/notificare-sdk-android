package re.notifica.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo

class NotificareOptions(context: Context) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

    val crashReportsEnabled: Boolean
        get() {
            return info.metaData.getBoolean("re.notifica.crash_reports_enabled", true)
        }

    val notificationActionLabelPrefix: String?
        get() {
            return info.metaData.getString("re.notifica.action_label_prefix", null)
        }
}
