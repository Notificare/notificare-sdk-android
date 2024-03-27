package re.notifica.geo.ktx

import kotlin.math.round

/**
 * Returns a list of [n] evenly spaced elements.
 *
 * @param n the number of elements to extract.
 */
internal fun <T> List<T>.takeEvenlySpaced(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }

    if (size <= n) {
        return this
    }

    val interval = (size - 1) / (n - 1.0)
    val elements = mutableListOf<T>()

    for (i in 0..<n) {
        val index = round(interval * i).toInt()
        elements.add(this[index])
    }

    return elements
}
