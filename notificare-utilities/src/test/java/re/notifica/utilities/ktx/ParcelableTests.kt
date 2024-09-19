package re.notifica.utilities.ktx

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
public class ParcelableTests {
    @Test
    public fun testPutAndGetParcelableFromIntent() {
        val testParcelable = TestParcelable("testData")

        val intent = Intent()
        intent.putExtra("key", testParcelable)

        val result: TestParcelable? = intent.parcelable("key")

        assertEquals(testParcelable, result)
    }

    @Test
    public fun testPutAndGetParcelableArrayListFromIntent() {
        val testParcelableList = arrayListOf(TestParcelable("testData"), TestParcelable("otherTestData"))

        val intent = Intent()
        intent.putParcelableArrayListExtra("key", testParcelableList)

        val result: ArrayList<TestParcelable>? = intent.parcelableArrayList("key")

        assertEquals(testParcelableList, result)
    }

    @Test
    public fun testPutAndGetParcelableFromBundle() {
        val testParcelable = TestParcelable("Test Data")

        val bundle = Bundle()
        bundle.putParcelable("key", testParcelable)

        val result: TestParcelable? = bundle.parcelable("key")

        assertEquals(testParcelable, result)
    }

    @Test
    public fun testPutAndGetParcelableArrayListFromBundle() {
        val testParcelableList = arrayListOf(TestParcelable("Data1"), TestParcelable("Data2"))

        val bundle = Bundle()
        bundle.putParcelableArrayList("key", testParcelableList)

        val result: ArrayList<TestParcelable>? = bundle.parcelableArrayList("key")

        assertEquals(testParcelableList, result)
    }

    @Test
    public fun testMapParcel() {
        val parcel = Parcel.obtain()
        val testMap = mutableMapOf("key" to TestParcelable("Test Data"))

        parcel.writeMap(testMap)

        parcel.setDataPosition(0)

        val resultMap = mutableMapOf<String, TestParcelable>()
        parcel.map(resultMap, TestParcelable::class.java.classLoader, String::class.java, TestParcelable::class.java)

        assertEquals(testMap, resultMap)

        parcel.recycle()
    }
}

private data class TestParcelable(
    val data: String
): Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString() ?: "")

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(data)
    }

    companion object CREATOR : Parcelable.Creator<TestParcelable> {
        override fun createFromParcel(parcel: Parcel): TestParcelable {
            return TestParcelable(parcel)
        }

        override fun newArray(size: Int): Array<TestParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
