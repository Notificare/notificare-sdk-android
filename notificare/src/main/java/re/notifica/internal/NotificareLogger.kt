package re.notifica.internal

import android.os.Build
import android.util.Log
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.NotificareConfigurationProvider
import java.io.PrintWriter
import java.io.StringWriter
import java.util.regex.Pattern

@InternalNotificareApi
public object NotificareLogger {

    private const val TAG = "Notificare"
    private const val MAX_TAG_LENGTH = 23
    private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    private val IGNORE_FQDN = listOf(
        Notificare::class.java.name,
        NotificareLogger::class.java.name,
        NotificareConfigurationProvider::class.java.name
    )

    private val hasDebugLoggingEnabled: Boolean
        get() = Notificare.options?.debugLoggingEnabled ?: false

    public fun debug(message: String, t: Throwable? = null): Unit = log(Log.DEBUG, message, t)

    public fun info(message: String, t: Throwable? = null): Unit = log(Log.INFO, message, t)

    public fun warning(message: String, t: Throwable? = null): Unit = log(Log.WARN, message, t)

    public fun error(message: String, t: Throwable? = null): Unit = log(Log.ERROR, message, t)

    private fun log(priority: Int, message: String, t: Throwable?) {
        val canLog = hasDebugLoggingEnabled || priority >= Log.INFO
        if (!canLog) return

        @Suppress("NAME_SHADOWING") val message = message
            .let { // transform with stack trace
                if (t != null) {
                    it + "\n" + getStackTraceString(t)
                } else {
                    it
                }
            }
            .let { // transform with internal tag
                if (hasDebugLoggingEnabled) {
                    "[$internalTag] $it"
                } else {
                    it
                }
            }

        Log.println(priority, TAG, message)
    }

    private val internalTag: String
        get() = Throwable().stackTrace
            .first { it.className !in IGNORE_FQDN }
            .let(::createStackElementTag)
            .replace("ModuleImpl", "")
            .replace("Impl", "")

    private fun createStackElementTag(element: StackTraceElement): String {
        var tag = element.className.substringAfterLast('.')
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }

        // Tag length limit was removed in API 24.
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }
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
