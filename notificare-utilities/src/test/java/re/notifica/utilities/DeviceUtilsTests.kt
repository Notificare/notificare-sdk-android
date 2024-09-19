package re.notifica.utilities

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class DeviceUtilsTests {
    @Test
    public fun testDeviceString() {
        assertEquals("${Build.MANUFACTURER} ${Build.MODEL}", deviceString)
    }

    @Test
    public fun testDeviceLanguage() {
        Locale.setDefault(Locale("pt", "PT"))
        assertEquals("pt", deviceLanguage)
    }

    @Test
    public fun testDeviceRegion() {
        Locale.setDefault(Locale("pt", "PT"))
        assertEquals("PT", deviceRegion)
    }

    @Test
    public fun testOsVersion() {
        assertEquals(Build.VERSION.RELEASE, osVersion)
    }

    @Test
    public fun testTimeZoneOffset() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"))

        val expectedOffset = 2.0
        assertEquals(expectedOffset, timeZoneOffset, 0.0)
    }
}
