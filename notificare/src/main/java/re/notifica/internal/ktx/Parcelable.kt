package re.notifica.internal.ktx

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import re.notifica.InternalNotificareApi

@InternalNotificareApi
public inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key)
    }
}

@InternalNotificareApi
public inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayListExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
    }
}

@InternalNotificareApi
public inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key)
    }
}

@InternalNotificareApi
public inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayList(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
    }
}

@InternalNotificareApi
public inline fun <reified K, reified V> Parcel.map(
    outVal: MutableMap<K, V>,
    classLoader: ClassLoader? = V::class.java.classLoader,
    klassKey: Class<K> = K::class.java,
    klassValue: Class<V> = V::class.java,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        readMap(outVal, V::class.java.classLoader)

        return
    }

    readMap(outVal, classLoader, klassKey, klassValue)
}
