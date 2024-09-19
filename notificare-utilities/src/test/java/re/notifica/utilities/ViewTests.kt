package re.notifica.utilities

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class ViewTests {
    private lateinit var rootView: FrameLayout
    private lateinit var testView: View
    private var wasCalled = false

    @Before
    public fun setup() {
        wasCalled = false

        val activity = Robolectric.buildActivity(TestActivity::class.java).create().get()
        rootView = FrameLayout(activity)
        testView = View(activity)
        rootView.addView(testView)
        activity.setContentView(rootView)
    }

    @Test
    public fun testWaitForLayoutAfterViewLaidOut() {
        testView.layout(0, 0, 100, 100)

        testView.waitForLayout {
            wasCalled = true
        }

        assertTrue(wasCalled)
    }

    @Test
    public fun testWaitForLayoutBeforeViewLaidOut() {
        testView.waitForLayout {
            wasCalled = true
        }

        testView.viewTreeObserver.dispatchOnGlobalLayout()

        // Assert
        assertTrue(wasCalled)
    }
}

private class TestActivity : Activity()
