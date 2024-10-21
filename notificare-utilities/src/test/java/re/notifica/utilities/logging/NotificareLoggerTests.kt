package re.notifica.utilities.logging

import android.util.Log
import org.junit.After
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
        logger = NotificareLogger("Notificare")
        logger.hasDebugLoggingEnabled = true

        MockitoAnnotations.openMocks(this)
        mockLog = mockStatic(Log::class.java)
    }

    @After
    public fun tearDown() {
        logger.labelClassIgnoreList = listOf()
        mockLog.close()
    }

    @Test
    public fun testDebugLogging() {
        val message = "Debug message"

        logger.debug(message)

        mockLog.verify {
            Log.println(Log.DEBUG, "Notificare", "[NotificareLoggerTests] $message")
        }
    }

    @Test
    public fun testInfoLogging() {
        val message = "Info message"

        logger.info(message)

        mockLog.verify { Log.println(Log.INFO, "Notificare", "[NotificareLoggerTests] $message") }
    }

    @Test
    public fun testErrorLogging() {
        val throwable = RuntimeException("Test exception")
        val message = "Error message"
        val expectedStackTrace = getStackTraceString(throwable)

        logger.error(message, throwable)

        val expectedMessage = "$message\n$expectedStackTrace"

        mockLog.verify { Log.println(Log.ERROR, "Notificare", "[NotificareLoggerTests] $expectedMessage") }
    }

    @Test
    public fun testDebugLoggingDisabled() {
        val message = "[NotificareLoggerTest] Message that should not be logged"
        logger.hasDebugLoggingEnabled = false
        logger.debug(message)

        mockLog.verify({ Log.println(Log.DEBUG, "Notificare", message) }, times(0))
    }

    @Test
    public fun testDebugLoggingWithLabelIgnore() {
        val message = "Debug message"
        logger.labelClassIgnoreList = listOf(
            NotificareLoggerTests::class,
        )
        logger.debug(message)

        mockLog.verify {
            Log.println(Log.DEBUG, "Notificare", message)
        }
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = StringWriter(256)
        val pw = PrintWriter(sw, false)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}
