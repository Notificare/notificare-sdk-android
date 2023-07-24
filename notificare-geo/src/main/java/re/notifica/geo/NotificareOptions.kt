package re.notifica.geo

import re.notifica.internal.NotificareOptions

public val NotificareOptions.monitoredRegionsLimit: Int?
    get() {
        if (!metadata.containsKey("re.notifica.geo.monitored_regions_limit")) return null
        return metadata.getInt("re.notifica.geo.monitored_regions_limit")
    }
