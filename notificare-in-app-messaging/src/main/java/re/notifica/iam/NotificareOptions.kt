package re.notifica.iam

import re.notifica.internal.NotificareOptions

public val NotificareOptions.backgroundGracePeriodMillis: Long?
    get() {
        if (metadata.containsKey("re.notifica.iam.background_grace_period_millis")) {
            return metadata.getInt("re.notifica.iam.background_grace_period_millis", 0).toLong()
        }

        return null
    }
