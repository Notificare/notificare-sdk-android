package re.notifica.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareDoNotDisturbTest {
    @Test
    public fun testNotificareDoNotDisturbSerialization() {
        val dnd = NotificareDoNotDisturb(
            start = NotificareTime(hours = 20, minutes = 0),
            end = NotificareTime(hours = 21, minutes = 0)
        )

        val convertedDnd = NotificareDoNotDisturb.fromJson(dnd.toJson())

        assertEquals(dnd, convertedDnd)
    }
}
