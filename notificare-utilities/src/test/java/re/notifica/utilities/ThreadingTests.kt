package re.notifica.utilities

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class ThreadingTests {
    private var wasCalled = false

    @Test
    public fun testOnMainThreadHandler() {
        onMainThread {
            wasCalled = true
        }

        Robolectric.flushForegroundThreadScheduler()

        assertTrue(wasCalled)
    }
}
