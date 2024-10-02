package re.notifica.utilities.networking

import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

public val Exception.isRecoverable: Boolean
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
