package re.notifica.utilities.parcel

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import re.notifica.utilities.parcel.IntentTests.TestEnum

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class BundleTests {

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
    public fun testPutAndGetInvalidValueFromBundle() {
        val bundle = Bundle()
        bundle.putInt("testKey", -1)

        val result = bundle.getEnum<TestEnum>("testKey")

        assertNull(result)
    }
}
