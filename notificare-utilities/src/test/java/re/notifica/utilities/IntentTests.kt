package re.notifica.utilities

import android.content.Intent
import android.os.Bundle
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
    public fun testPutAndGetEnumFromBundle() {
        val bundle = Bundle()
        bundle.putEnum("testKey", TestEnum.SECOND)

        val result = bundle.getEnum<TestEnum>("testKey")

        assertEquals(TestEnum.SECOND, result)
    }

    @Test
    public fun testPutAndGetNullFromBundle() {
        val bundle = Bundle()
        bundle.putEnum<TestEnum>("testKey", null)

        val result = bundle.getEnum<TestEnum>("testKey")

        assertNull(result)
    }

    @Test
    public fun testPutAndGetInvalidValueFromIntent() {
        val intent = Intent()
        intent.putExtra("testKey", -1)

        val result = intent.getEnumExtra<TestEnum>("testKey")

        assertNull(result)
    }

    @Test
    public fun testPutAndGetInvalidValueFromVundle() {
        val bundle = Bundle()
        bundle.putInt("testKey", -1)

        val result = bundle.getEnum<TestEnum>("testKey")

        assertNull(result)
    }
}
