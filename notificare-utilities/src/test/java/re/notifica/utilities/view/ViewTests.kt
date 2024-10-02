package re.notifica.utilities.view

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class ViewTests {
    private lateinit var testView: View

    @Before
    public fun setup() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).create().get()
        testView = FrameLayout(activity)
        activity.setContentView(testView)
    }

    @Test
    public fun testWaitForLayoutAfterViewLaidOut() {
        var wasCalled = false

        testView.layout(0, 0, 100, 100)

        testView.viewTreeObserver.dispatchOnGlobalLayout()

        testView.waitForLayout {
            wasCalled = true
        }

        Assert.assertTrue(wasCalled)
    }

    @Test
    public fun testWaitForLayoutBeforeViewLaidOut() {
        var wasCalled = false

        testView.waitForLayout {
            wasCalled = true
        }

        testView.viewTreeObserver.dispatchOnGlobalLayout()

        // Assert
        Assert.assertTrue(wasCalled)
    }
}

private class TestActivity : Activity()
