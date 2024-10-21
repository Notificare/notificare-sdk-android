package re.notifica.utilities.threading

import android.os.Looper
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class MainThreadTests {
    @Test
    public fun testOnMainThreadHandler() {
        var wasCalled = false
        var wasMainThread = false

        onMainThread {
            wasCalled = true
            wasMainThread = Thread.currentThread() == Looper.getMainLooper().thread
        }

        Robolectric.flushForegroundThreadScheduler()

        assertTrue(wasCalled)
        assertTrue(wasMainThread)
    }
}
