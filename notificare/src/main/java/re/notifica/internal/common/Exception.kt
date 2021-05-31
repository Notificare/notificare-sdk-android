package re.notifica.internal.common

import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

internal val Exception.recoverable: Boolean
    get() {
        return when (this) {
            // Network failures
            is UnknownHostException,
            is SocketException,
            is TimeoutException -> true
            is SSLException -> {
                this.toString().lowercase().contains("connection reset by peer")
            }
            // Cancelled worker
            is CancellationException -> true
            else -> false
        }
    }
