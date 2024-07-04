package re.notifica.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareDeviceTest {
    @Test
    public fun testNotificareDeviceSerialization() {
        val device = NotificareDevice(
            id = "testId",
            userId = "testUserId",
            userName = "testUserName",
            timeZoneOffset = 1.5,
            dnd = NotificareDoNotDisturb(
                start = NotificareTime(hours = 20, minutes = 0),
                end = NotificareTime(hours = 21, minutes = 0)
            ),
            userData = mapOf()
        )

        val convertedDevice = NotificareDevice.fromJson(device.toJson())

        assertEquals(device, convertedDevice)
    }
}
