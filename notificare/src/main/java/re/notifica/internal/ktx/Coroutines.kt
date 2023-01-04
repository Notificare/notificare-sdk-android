package re.notifica.internal.ktx

import kotlinx.coroutines.*
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.NotificareCallback

private val ncCoroutineScope: CoroutineScope by lazy {
    CoroutineScope(Dispatchers.IO + SupervisorJob())
}

@Suppress("unused")
@InternalNotificareApi
public val Notificare.coroutineScope: CoroutineScope
    get() = ncCoroutineScope

@PublishedApi
internal fun <T> awaitSuspend(fn: (suspend () -> T), callback: NotificareCallback<T>) {
    Notificare.coroutineScope.launch {
        try {
            val result = fn()

            withContext(Dispatchers.Main) {
                callback.onSuccess(result)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onFailure(e)
            }
        }
    }
}

@InternalNotificareApi
public fun <T> toCallbackFunction(fn: suspend () -> T): (callback: NotificareCallback<T>) -> Unit = { callback ->
    awaitSuspend(fn, callback)
}

@InternalNotificareApi
public fun <A, T> toCallbackFunction(
    fn: suspend (A) -> T
): (a: A, callback: NotificareCallback<T>) -> Unit = { a, callback ->
    awaitSuspend(suspend { fn(a) }, callback)
}

@InternalNotificareApi
public fun <A, B, T> toCallbackFunction(
    fn: suspend (A, B) -> T
): (a: A, b: B, callback: NotificareCallback<T>) -> Unit = { a, b, callback ->
    awaitSuspend(suspend { fn(a, b) }, callback)
}

@InternalNotificareApi
public fun <A, B, C, T> toCallbackFunction(
    fn: suspend (A, B, C) -> T
): (a: A, b: B, c: C, callback: NotificareCallback<T>) -> Unit = { a, b, c, callback ->
    awaitSuspend(suspend { fn(a, b, c) }, callback)
}
