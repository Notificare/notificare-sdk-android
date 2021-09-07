package re.notifica.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import re.notifica.InternalNotificareApi

public class NotificareOptions internal constructor(context: Context) {

    @InternalNotificareApi
    public val info: ApplicationInfo = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA
    )

    public val crashReportsEnabled: Boolean
        get() {
            return info.metaData?.getBoolean("re.notifica.crash_reports_enabled", true) ?: true
        }

    public val notificationActionLabelPrefix: String?
        get() {
            return info.metaData?.getString("re.notifica.action_label_prefix", null)
        }

    public val preferredMobileServices: String?
        get() {
            return info.metaData?.getString(
                "re.notifica.preferred_mobile_services",
                null,
            )
        }
}
