package re.notifica.push.models

import org.json.JSONObject
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
            content = JSONObject("""{"testJson":"testValue"}"""),
            final = true,
            dismissalDate = Date(),
            timestamp = Date()
        )

        val convertedLiveActivityUpdate = NotificareLiveActivityUpdate.fromJson(liveActivityUpdate.toJson())

        assertEquals(liveActivityUpdate.activity, convertedLiveActivityUpdate.activity)
        assertEquals(liveActivityUpdate.title, convertedLiveActivityUpdate.title)
        assertEquals(liveActivityUpdate.subtitle, convertedLiveActivityUpdate.subtitle)
        assertEquals(liveActivityUpdate.message, convertedLiveActivityUpdate.message)
        assertEquals(liveActivityUpdate.content.toString(), convertedLiveActivityUpdate.content.toString())
        assertEquals(liveActivityUpdate.final, convertedLiveActivityUpdate.final)
        assertEquals(liveActivityUpdate.dismissalDate, convertedLiveActivityUpdate.dismissalDate)
        assertEquals(liveActivityUpdate.timestamp, convertedLiveActivityUpdate.timestamp)
    }

    @Test
    public fun testNotificareLiveActivityUpdateSerializationWithNullProps() {
        val liveActivityUpdate = NotificareLiveActivityUpdate(
            activity = "testActivity",
            title = null,
            subtitle = null,
            message = null,
            content = null,
            final = true,
            dismissalDate = null,
            timestamp = Date()
        )

        val convertedLiveActivityUpdate = NotificareLiveActivityUpdate.fromJson(liveActivityUpdate.toJson())

        assertEquals(liveActivityUpdate, convertedLiveActivityUpdate)
    }
}
