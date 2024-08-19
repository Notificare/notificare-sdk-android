package re.notifica.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.os.bundleOf
import re.notifica.InternalNotificareApi
import re.notifica.internal.ktx.applicationInfo

public class NotificareOptions internal constructor(context: Context) {

    @InternalNotificareApi
    public val info: ApplicationInfo = context.packageManager.applicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA
    )

    @InternalNotificareApi
    public val metadata: Bundle = info.metaData ?: bundleOf()

    public val debugLoggingEnabled: Boolean
        get() {
            return metadata.getBoolean("re.notifica.debug_logging_enabled", false)
        }

    public val crashReportsEnabled: Boolean
        get() {
            return metadata.getBoolean("re.notifica.crash_reports_enabled", true)
        }

    public val notificationActionLabelPrefix: String?
        get() {
            return metadata.getString("re.notifica.action_label_prefix", null)
        }
}
