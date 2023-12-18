package re.notifica.internal.ktx

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.core.os.ParcelCompat
import re.notifica.InternalNotificareApi

@InternalNotificareApi
public inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    return IntentCompat.getParcelableExtra(this, key, T::class.java)
}

@InternalNotificareApi
public inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? {
    return IntentCompat.getParcelableArrayListExtra(this, key, T::class.java)
}

@InternalNotificareApi
public inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? {
    return BundleCompat.getParcelable(this, key, T::class.java)
}

@InternalNotificareApi
public inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? {
    return BundleCompat.getParcelableArrayList(this, key, T::class.java)
}

@InternalNotificareApi
public inline fun <reified K, reified V> Parcel.map(
    outVal: MutableMap<K, V>,
    classLoader: ClassLoader? = V::class.java.classLoader,
    klassKey: Class<K> = K::class.java,
    klassValue: Class<V> = V::class.java,
) {
    return ParcelCompat.readMap(this, outVal, classLoader, klassKey, klassValue)
}
