package re.notifica.utilities.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

public class EncodeNullsTest {

    private lateinit var moshi: Moshi
    private lateinit var encodeNullsFactory: EncodeNullsFactory

    @Before
    public fun setUp() {
        encodeNullsFactory = EncodeNullsFactory()
        moshi = Moshi.Builder()
            .add(encodeNullsFactory)
            .build()
    }

    @Test
    public fun testSerializationWithEncodeNullsAnnotation() {
        val testObject = TestEncodeNullClass(name = null, age = null)

        val jsonAdapter: JsonAdapter<TestEncodeNullClass> = moshi.adapter(TestEncodeNullClass::class.java)
        val json = jsonAdapter.toJson(testObject)

        assertEquals("""{"name":null,"age":null}""", json)
    }
}

@EncodeNulls
@JsonClass(generateAdapter = true)
internal data class TestEncodeNullClass(val name: String?, val age: Int?)
