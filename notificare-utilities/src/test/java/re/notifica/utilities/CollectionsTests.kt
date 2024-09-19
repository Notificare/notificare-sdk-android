package re.notifica.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class CollectionsTests {
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
