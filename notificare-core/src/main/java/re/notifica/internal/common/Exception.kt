package re.notifica.internal.common

import java.net.SocketException
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

internal val Exception.recoverable: Boolean
    get() {
        return when (this) {
            is UnknownHostException,
            is SocketException,
            is TimeoutException -> true
            is SSLException -> {
                this.toString().toLowerCase(Locale.ROOT).contains("connection reset by peer")
            }
            else -> false
        }
    }
