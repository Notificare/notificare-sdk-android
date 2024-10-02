package re.notifica.utilities.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

public class UseDefaultsWhenNullFactoryTest {

    private lateinit var moshi: Moshi
    private lateinit var useDefaultsWhenNullFactory: UseDefaultsWhenNullFactory

    @Before
    public fun setUp() {
        useDefaultsWhenNullFactory = UseDefaultsWhenNullFactory()
        moshi = Moshi.Builder()
            .add(useDefaultsWhenNullFactory)
            .build()
    }

    @Test
    public fun testDeserializationWithUseDefaultsWhenNullAnnotation() {
        val json = """{"name":null}"""

        val adapter: JsonAdapter<TestUseDefaultsWhenNullClass> = moshi.adapter(TestUseDefaultsWhenNullClass::class.java)
        val result = adapter.fromJson(json)

        assertEquals(TestUseDefaultsWhenNullClass(name = "default", age = 0), result)
    }

    @Test
    public fun testSerializationWithUseDefaultsWhenNullAnnotation() {
        val testObject = TestUseDefaultsWhenNullClass()

        val adapter: JsonAdapter<TestUseDefaultsWhenNullClass> = moshi.adapter(TestUseDefaultsWhenNullClass::class.java)
        val json = adapter.toJson(testObject)

        assertEquals("""{"name":"default","age":0}""", json)
    }
}

@UseDefaultsWhenNull
@JsonClass(generateAdapter = true)
internal data class TestUseDefaultsWhenNullClass(val name: String = "default", val age: Int = 0)
