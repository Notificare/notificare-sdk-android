package re.notifica.push.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareSystemNotificationTest {
    @Test
    public fun testNotificareSystemNotificationSerialization() {
        val notification = NotificareSystemNotification(
            id = "testId",
            type = "testType",
            extra = mapOf("testKey" to "testValue")
        )

        val convertedNotification = NotificareSystemNotification.fromJson(notification.toJson())

        assertEquals(notification, convertedNotification)
    }
}
