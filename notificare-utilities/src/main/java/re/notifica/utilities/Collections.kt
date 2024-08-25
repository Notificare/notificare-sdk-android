package re.notifica.utilities

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
