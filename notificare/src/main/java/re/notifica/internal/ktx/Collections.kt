package re.notifica.internal.ktx

import re.notifica.InternalNotificareApi

@InternalNotificareApi
public inline fun <reified K, reified V> Map<*, *>.cast(): Map<K, V> {
    return this.mapNotNull { entry ->
        val key = entry.key as? K
        val value = entry.value as? V

        if (key == null || value == null) null
        else key to value
    }.toMap()
}
