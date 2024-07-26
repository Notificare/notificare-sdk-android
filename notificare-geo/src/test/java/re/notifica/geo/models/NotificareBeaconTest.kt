package re.notifica.geo.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareBeaconTest {
    @Test
    public fun testNotificareBeaconSerialization() {
        val beacon = NotificareBeacon(
            id = "testId",
            name = "testName",
            major = 1,
            minor = 1,
            triggers = true,
            proximity = NotificareBeacon.Proximity.NEAR
        )

        val convertedBeacon = NotificareBeacon.fromJson(beacon.toJson())

        assertEquals(beacon, convertedBeacon)
    }

    @Test
    public fun testNotificareBeaconSerializationWithNullProps() {
        val beacon = NotificareBeacon(
            id = "testId",
            name = "testName",
            major = 1,
            minor = null,
            triggers = true,
            proximity = NotificareBeacon.Proximity.NEAR
        )

        val convertedBeacon = NotificareBeacon.fromJson(beacon.toJson())

        assertEquals(beacon, convertedBeacon)
    }
}
