package re.notifica.utilities.logging

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

public class NotificareLogger(
    private val hasDebugLoggingEnabled: Boolean = false,
    private val internalTag: String? = null
) {

    private val tag = "Notificare"

    public fun debug(message: String, t: Throwable? = null): Unit = log(Log.DEBUG, message, t)

    public fun info(message: String, t: Throwable? = null): Unit = log(Log.INFO, message, t)

    public fun warning(message: String, t: Throwable? = null): Unit = log(Log.WARN, message, t)

    public fun error(message: String, t: Throwable? = null): Unit = log(Log.ERROR, message, t)

    private fun log(priority: Int, message: String, t: Throwable?) {
        val canLog = hasDebugLoggingEnabled || priority >= Log.INFO
        if (!canLog) return

        @Suppress("NAME_SHADOWING")
        val message = message
            .let { // transform with stack trace
                if (t != null) {
                    it + "\n" + getStackTraceString(t)
                } else {
                    it
                }
            }
            .let { // transform with internal tag
                if (hasDebugLoggingEnabled && internalTag != null) {
                    "[$internalTag] $it"
                } else {
                    it
                }
            }

        Log.println(priority, tag, message)
    }

    private fun getStackTraceString(t: Throwable): String {
        // Don't replace this with Log.getStackTraceString() - it hides
        // UnknownHostException, which is not what we want.
        val sw = StringWriter(256)
        val pw = PrintWriter(sw, false)

        t.printStackTrace(pw)
        pw.flush()

        return sw.toString()
    }
}
