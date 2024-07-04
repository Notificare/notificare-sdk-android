package re.notifica.geo.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareLocationTest {
    @Test
    public fun testNotificareLocationSerialization() {
        val location = NotificareLocation(
            latitude = 1.5,
            longitude = 1.5,
            altitude = 1.5,
            course = 1.5,
            speed = 1.5,
            horizontalAccuracy = 1.5,
            verticalAccuracy = 1.5,
            timestamp = Date()
        )

        val convertedLocation = NotificareLocation.fromJson(location.toJson())

        assertEquals(location, convertedLocation)
    }
}
