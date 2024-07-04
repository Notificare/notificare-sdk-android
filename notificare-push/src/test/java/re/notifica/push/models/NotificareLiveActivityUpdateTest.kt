package re.notifica.push.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareLiveActivityUpdateTest {
    @Test
    public fun testNotificareLiveActivityUpdateSerialization() {
        val liveActivityUpdate = NotificareLiveActivityUpdate(
            activity = "testActivity",
            title = "testTitle",
            subtitle = "testSubtitle",
            message = "testMessage",
            content = null,
            final = true,
            dismissalDate = Date(),
            timestamp = Date()
        )

        val convertedLiveActivityUpdate = NotificareLiveActivityUpdate.fromJson(liveActivityUpdate.toJson())

        assertEquals(liveActivityUpdate, convertedLiveActivityUpdate)
    }
}
