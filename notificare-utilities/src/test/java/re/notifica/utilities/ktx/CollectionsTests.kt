package re.notifica.utilities.ktx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class CollectionsTests {

    @Test
    public fun testSuccessfulMapCast() {
        val map = mapOf<Any, Any>(
            "key1" to 1,
            "key2" to 2
        )

        val result: Map<String, Int> = map.cast()

        val expectedMap = mapOf(
            "key1" to 1,
            "key2" to 2
        )

        assertEquals(expectedMap, result)
    }

    @Test
    public fun testPartialUnsuccessfulMapCast() {
        val map = mapOf<Any, Any>(
            "key1" to 1,
            2 to "value2",
            "key3" to 3
        )

        val result: Map<String, Int> = map.cast()

        val expectedMap = mapOf(
            "key1" to 1,
            "key3" to 3
        )

        assertEquals(expectedMap, result)
    }

    @Test
    public fun testCompleteUnsuccessfulMapCast() {
        val map = mapOf<Any, Any>(
            1 to 2,
            3.14 to "value"
        )

        val result: Map<String, Int> = map.cast()

        assertTrue(result.isEmpty())
    }

    @Test
    public fun testEmptyMapCast() {
        val map = emptyMap<Any, Any>()

        val result: Map<String, Int> = map.cast()

        assertTrue(result.isEmpty())
    }
}
