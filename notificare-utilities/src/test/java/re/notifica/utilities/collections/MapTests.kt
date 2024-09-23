package re.notifica.utilities.collections

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class MapTests {
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

    @Test
    public fun testMapNullFilterWithNoNulls() {
        val expectedMap = mapOf(1 to "one", 2 to "two", 3 to "three")
        val filteredMap = expectedMap.filterNotNull { it.value }

        assertEquals(expectedMap, filteredMap)
    }

    @Test
    public fun testMapNullFilterWithAllNulls() {
        val map = mapOf(1 to null, 2 to null, 3 to null)
        val filteredMap = map.filterNotNull { it.value }

        val expectedMap = mapOf<Int, String>()

        assertEquals(expectedMap, filteredMap)
    }

    @Test
    public fun testMapNullFilterWithSomeNulls() {
        val map = mapOf(1 to null, 2 to null, 3 to "three")
        val filteredMap = map.filterNotNull { it.value }

        val expectedMap = mapOf(3 to "three")

        assertEquals(expectedMap, filteredMap)
    }

    @Test
    public fun testEmptyMapNullFilter() {
        val expectedMap = emptyMap<Int, String>()
        val filteredMap = expectedMap.filterNotNull { it.value }

        assertEquals(expectedMap, filteredMap)
    }
}
