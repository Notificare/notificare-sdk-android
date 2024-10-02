package re.notifica.utilities.parcel

import android.os.Parcel
import androidx.core.os.ParcelCompat

public inline fun <reified K, reified V> Parcel.map(
    outVal: MutableMap<K, V>,
    classLoader: ClassLoader? = V::class.java.classLoader,
    klassKey: Class<K> = K::class.java,
    klassValue: Class<V> = V::class.java,
) {
    return ParcelCompat.readMap(this, outVal, classLoader, klassKey, klassValue)
}
