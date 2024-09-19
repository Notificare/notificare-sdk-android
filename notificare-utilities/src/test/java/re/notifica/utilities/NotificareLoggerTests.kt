package re.notifica.utilities

import android.util.Log
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import java.io.PrintWriter
import java.io.StringWriter

public class NotificareLoggerTests {
    private lateinit var logger: NotificareLogger
    private lateinit var mockLog: MockedStatic<Log>

    @Before
    public fun setup() {
        MockitoAnnotations.openMocks(this)
        mockLog = mockStatic(Log::class.java)
    }

    @Test
    public fun testDebugLogging() {
        val message = "Debug message"

        logger = NotificareLogger(true)
        logger.debug(message)

        mockLog.verify { Log.println(Log.DEBUG, "Notificare", message) }
        mockLog.close()
    }

    @Test
    public fun testInfoLogging() {
        val message = "Info message"

        logger = NotificareLogger(true)
        logger.info(message)

        mockLog.verify { Log.println(Log.INFO, "Notificare", message) }
        mockLog.close()
    }

    @Test
    public fun testErrorLogging() {
        val throwable = RuntimeException("Test exception")
        val message = "Error message"
        val expectedStackTrace = getStackTraceString(throwable)

        logger = NotificareLogger(true)
        logger.error(message, throwable)

        val expectedMessage = "$message\n$expectedStackTrace"

        mockLog.verify { Log.println(Log.ERROR, "Notificare", expectedMessage) }
        mockLog.close()
    }

    @Test
    public fun testInfoLoggingWithInternalTag() {
        val internalTag = "NotificareTest"
        val message = "Info message with internal tag"

        val taggedMessage = "[$internalTag] $message"

        logger = NotificareLogger(true, internalTag)
        logger.info(message)

        mockLog.verify { Log.println(Log.INFO, "Notificare", taggedMessage) }
        mockLog.close()
    }

    @Test
    public fun testErrorLoggingWithInternalTag() {
        val throwable = RuntimeException("Test exception")
        val internalTag = "NotificareTest"
        val message = "Error message"
        val taggedMessage = "[$internalTag] $message"
        val expectedStackTrace = getStackTraceString(throwable)

        logger = NotificareLogger(true, internalTag)
        logger.error(message, throwable)

        val expectedMessage = "$taggedMessage\n$expectedStackTrace"

        mockLog.verify { Log.println(Log.ERROR, "Notificare", expectedMessage) }
        mockLog.close()
    }

    @Test
    public fun testDebugLoggingWithInternalTag() {
        val internalTag = "NotificareTest"
        val message = "Debug message with internal tag"

        val taggedMessage = "[$internalTag] $message"

        logger = NotificareLogger(true, internalTag)
        logger.debug(message)

        mockLog.verify { Log.println(Log.DEBUG, "Notificare", taggedMessage) }
        mockLog.close()
    }

    @Test
    public fun testDebugLoggingDisabled() {
        logger = NotificareLogger(false)
        val message = "Message that should not be logged"

        logger.debug(message)

        mockLog.verify({ Log.println(Log.DEBUG, "Notificare", message) }, times(0))
        mockLog.close()
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = StringWriter(256)
        val pw = PrintWriter(sw, false)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}
