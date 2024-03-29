package re.notifica.internal.common

import android.content.Intent
import android.os.Bundle
import re.notifica.InternalNotificareApi

@InternalNotificareApi
public inline fun <reified T : Enum<T>> Intent.putEnumExtra(name: String, value: T?): Intent {
    return putExtra(name, value?.ordinal)
}

@InternalNotificareApi
public inline fun <reified T : Enum<T>> Intent.getEnumExtra(name: String): T? {
    return getIntExtra(name, -1)
        .takeUnless { it == -1 }
        ?.let { T::class.java.enumConstants?.get(it) }
}


@InternalNotificareApi
public inline fun <reified T : Enum<T>> Bundle.putEnum(name: String, value: T?) {
    if (value != null) {
        return putInt(name, value.ordinal)
    }
}

@InternalNotificareApi
public inline fun <reified T : Enum<T>> Bundle.getEnum(name: String): T? {
    return getInt(name, -1)
        .takeUnless { it == -1 }
        ?.let { T::class.java.enumConstants?.get(it) }
}
