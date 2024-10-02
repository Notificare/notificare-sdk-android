package re.notifica.utilities.networking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException
import kotlin.coroutines.cancellation.CancellationException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class ExceptionTests {
    @Test
    public fun testUnknownHostExceptionIsRecoverable() {
        val exception = UnknownHostException()
        assertTrue(exception.isRecoverable)
    }

    @Test
    public fun testSocketExceptionIsRecoverable() {
        val exception = SocketException()
        assertTrue(exception.isRecoverable)
    }

    @Test
    public fun testTimeoutExceptionIsRecoverable() {
        val exception = TimeoutException()
        assertTrue(exception.isRecoverable)
    }

    @Test
    public fun testSSLExceptionWithCorrectMessageIsNotRecoverable() {
        val exception = SSLException("connection reset by peer")
        assertTrue(exception.isRecoverable)
    }

    @Test
    public fun testSSLExceptionWithIncorrectMessageIsRecoverable() {
        val exception = SSLException("certificate expired")
        assertFalse(exception.isRecoverable)
    }

    @Test
    public fun testCancellationExceptionIsRecoverable() {
        val exception = CancellationException()
        assertTrue(exception.isRecoverable)
    }

    @Test
    public fun testGenericExceptionIsNotRecoverable() {
        val exception = Exception()
        assertFalse(exception.isRecoverable)
    }

    @Test
    public fun testRuntimeExceptionIsNotRecoverable() {
        val exception = RuntimeException()
        assertFalse(exception.isRecoverable)
    }
}
