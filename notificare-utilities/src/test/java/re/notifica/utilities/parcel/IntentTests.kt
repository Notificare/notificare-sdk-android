package re.notifica.utilities.parcel

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class IntentTests {
    public enum class TestEnum {
        FIRST,
        SECOND,
        THIRD
    }

    @Test
    public fun testPutAndGetEnumFromIntent() {
        val intent = Intent()
        intent.putEnumExtra("testKey", TestEnum.FIRST)

        val result = intent.getEnumExtra<TestEnum>("testKey")

        assertEquals(TestEnum.FIRST, result)
    }

    @Test
    public fun testPutAndGetNullFromIntent() {
        val intent = Intent()
        intent.putEnumExtra<TestEnum>("testKey", null)

        val result = intent.getEnumExtra<TestEnum>("testKey")

        assertNull(result)
    }

    @Test
    public fun testPutAndGetInvalidValueFromIntent() {
        val intent = Intent()
        intent.putExtra("testKey", -1)

        val result = intent.getEnumExtra<TestEnum>("testKey")

        assertNull(result)
    }
}
