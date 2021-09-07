package re.notifica.internal.common

import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : Enum<T>> Intent.putEnumExtra(name: String, value: T?): Intent {
    return putExtra(name, value?.ordinal)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : Enum<T>> Intent.getEnumExtra(name: String): T? {
    return getIntExtra(name, -1)
        .takeUnless { it == -1 }
        ?.let { T::class.java.enumConstants?.get(it) }
}


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : Enum<T>> Bundle.putEnum(name: String, value: T?) {
    if (value != null) {
        return putInt(name, value.ordinal)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : Enum<T>> Bundle.getEnum(name: String): T? {
    return getInt(name, -1)
        .takeUnless { it == -1 }
        ?.let { T::class.java.enumConstants?.get(it) }
}
