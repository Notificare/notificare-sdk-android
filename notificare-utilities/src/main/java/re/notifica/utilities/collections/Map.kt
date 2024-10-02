package re.notifica.utilities.collections

public inline fun <reified K, reified V> Map<*, *>.cast(): Map<K, V> {
    return this.mapNotNull { entry ->
        val key = entry.key as? K
        val value = entry.value as? V

        if (key == null || value == null) null
        else key to value
    }.toMap()
}

public inline fun <K, V, R : Any> Map<out K, V>.filterNotNull(predicate: (Map.Entry<K, V>) -> R?): Map<K, R> {
    val result = LinkedHashMap<K, R>()
    for (entry in this) {
        val transformed = predicate(entry)
        if (transformed != null) {
            result[entry.key] = transformed
        }
    }
    return result
}
