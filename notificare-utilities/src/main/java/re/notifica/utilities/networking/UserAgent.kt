package re.notifica.utilities.networking

import android.content.Context
import android.os.Build
import re.notifica.utilities.content.applicationName
import re.notifica.utilities.content.applicationVersion

public fun Context.userAgent(sdkVersion: String): String {
    return "$applicationName/$applicationVersion Notificare/$sdkVersion Android/${Build.VERSION.RELEASE}"
}
