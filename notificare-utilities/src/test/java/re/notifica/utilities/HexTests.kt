package re.notifica.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class HexTests {
    @Test
    public fun testUUIDToByteArrayConversion() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val expectedByteArray = byteArrayOf(
            0x12, 0x3e, 0x45, 0x67, 0xe8.toByte(), 0x9b.toByte(), 0x12, 0xd3.toByte(),
            0xa4.toByte(), 0x56, 0x42, 0x66, 0x14, 0x17, 0x40, 0x00
        )

        val actualByteArray = uuid.toByteArray()

        assertEquals(expectedByteArray.size, actualByteArray.size)
        assertEquals(expectedByteArray.toList(), actualByteArray.toList())
    }

    @Test
    public fun testByteArrayToHexConversion() {
        val byteArray = byteArrayOf(
            0x12, 0x3e, 0x45, 0x67, 0xe8.toByte(), 0x9b.toByte(), 0x12, 0xd3.toByte(),
            0xa4.toByte(), 0x56, 0x42, 0x66, 0x14, 0x17, 0x40, 0x00
        )
        val expectedHexString = "123e4567e89b12d3a456426614174000"

        val actualHexString = byteArray.toHex()

        assertEquals(expectedHexString, actualHexString)
    }

    @Test
    public fun testUUIDToHexConversion() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val expectedHexString = "123e4567e89b12d3a456426614174000"

        val byteArray = uuid.toByteArray()
        val actualHexString = byteArray.toHex()

        assertEquals(expectedHexString, actualHexString)
    }
}
