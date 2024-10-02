package re.notifica.utilities.logging

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KClass

public class NotificareLogger(
    private val tag: String = "Notificare",
) {

    private val label: String?
        get() {
            @Suppress("detekt:ThrowingExceptionsWithoutMessageOrCause")
            return Throwable().stackTrace
                // Get the first reference outside of self
                .firstOrNull { it.className != NotificareLogger::class.java.name }
                // Remove nested references
                ?.let { it.className.split("$")[0] }
                // Apply the label class filtering
                ?.let { className -> if (labelClassIgnoreList.any { it.java.name == className }) null else className }
                // Transform the class name to the standard label
                ?.substringAfterLast('.')
                // Ignore the label when it's the same as the tag
                ?.let { if (it == tag) null else it }
        }

    public var hasDebugLoggingEnabled: Boolean = false
    public var labelClassIgnoreList: List<KClass<*>> = listOf()

    public fun debug(message: String, t: Throwable? = null): Unit =
        log(Log.DEBUG, message, t)

    public fun info(message: String, t: Throwable? = null): Unit =
        log(Log.INFO, message, t)

    public fun warning(message: String, t: Throwable? = null): Unit =
        log(Log.WARN, message, t)

    public fun error(message: String, t: Throwable? = null): Unit =
        log(Log.ERROR, message, t)

    private fun log(priority: Int, message: String, t: Throwable?) {
        val canLog = hasDebugLoggingEnabled || priority >= Log.INFO
        if (!canLog) return

        @Suppress("NAME_SHADOWING")
        val message = message
            // transform with stack trace
            .let {
                if (t != null) {
                    it + "\n" + getStackTraceString(t)
                } else {
                    it
                }
            }
            // transform with label
            .let {
                if (!hasDebugLoggingEnabled) return@let it
                val label = label ?: return@let it

                "[$label] $it"
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
