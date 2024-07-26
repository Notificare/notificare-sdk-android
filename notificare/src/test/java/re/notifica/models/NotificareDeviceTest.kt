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
            userData = mapOf("testKey" to "testValue")
        )

        val convertedDevice = NotificareDevice.fromJson(device.toJson())

        assertEquals(device, convertedDevice)
    }

    @Test
    public fun testNotificareDeviceSerializationWithNullProps() {
        val device = NotificareDevice(
            id = "testId",
            userId = null,
            userName = null,
            timeZoneOffset = 1.5,
            dnd = null,
            userData = mapOf("testKey" to "testValue")
        )

        val convertedDevice = NotificareDevice.fromJson(device.toJson())

        assertEquals(device, convertedDevice)
    }
}
