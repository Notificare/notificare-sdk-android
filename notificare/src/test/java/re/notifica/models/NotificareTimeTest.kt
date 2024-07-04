package re.notifica.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareTimeTest {
    @Test
    public fun testInvalidNotificareTimeInitialization() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificareTime(-1, -1)
        }
    }

    @Test
    public fun testInvalidStringNotificareTimeInitialization() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificareTime("21h30")
        }

        assertThrows(IllegalArgumentException::class.java) {
            NotificareTime(":")
        }

        assertThrows(IllegalArgumentException::class.java) {
            NotificareTime("21:30:45")
        }
    }

    @Test
    public fun testStringNotificareTimeInitialization() {
        val time = NotificareTime("21:30")

        assertEquals(21, time.hours)
        assertEquals(30, time.minutes)
    }

    @Test
    public fun testNotificareTimeFormat() {
        val time = NotificareTime("21:30")
        val timeString = time.format()

        assertEquals("21:30", timeString)
    }
}
