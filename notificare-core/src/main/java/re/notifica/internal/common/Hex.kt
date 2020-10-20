package re.notifica.internal.common

fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}
