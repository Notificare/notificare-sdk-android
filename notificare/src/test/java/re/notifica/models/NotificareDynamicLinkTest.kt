package re.notifica.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class NotificareDynamicLinkTest {
    @Test
    public fun testNotificareDynamicLinkSerialization() {
        val dynamicLink = NotificareDynamicLink(
            target = "testTarget"
        )

        val convertedDynamicLink = NotificareDynamicLink.fromJson(dynamicLink.toJson())

        assertEquals(dynamicLink, convertedDynamicLink)
    }
}
