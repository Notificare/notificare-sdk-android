package re.notifica.internal.common

import java.nio.ByteBuffer
import java.util.*

internal fun UUID.toByteArray(): ByteArray {
    return ByteBuffer.wrap(ByteArray(16))
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()
}

internal fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}
