package re.notifica.utilities
import java.nio.ByteBuffer
import java.util.*

public fun UUID.toByteArray(): ByteArray {
    return ByteBuffer.wrap(ByteArray(16))
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()
}

public fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}
