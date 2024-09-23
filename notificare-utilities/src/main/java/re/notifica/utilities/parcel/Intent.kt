package re.notifica.utilities.parcel

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat

public inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    return IntentCompat.getParcelableExtra(this, key, T::class.java)
}

public inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? {
    return IntentCompat.getParcelableArrayListExtra(this, key, T::class.java)
}

public inline fun <reified T : Enum<T>> Intent.putEnumExtra(name: String, value: T?): Intent {
    return putExtra(name, value?.ordinal)
}

public inline fun <reified T : Enum<T>> Intent.getEnumExtra(name: String): T? {
    return getIntExtra(name, -1)
        .takeUnless { it == -1 }
        ?.let { T::class.java.enumConstants?.get(it) }
}
