package re.notifica.utilities.ktx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ncCoroutineScope: CoroutineScope by lazy {
    CoroutineScope(Dispatchers.IO + SupervisorJob())
}

@Suppress("unused")
public val notificareCoroutineScope: CoroutineScope
    get() = ncCoroutineScope


@PublishedApi
internal fun <T> awaitSuspend(
    fn: (suspend () -> T),
    callbackSuccess: (T) -> Unit,
    callbackFailure: (Exception) -> Unit
) {
    notificareCoroutineScope.launch {
        try {
            val result = fn()

            withContext(Dispatchers.Main) {
                callbackSuccess(result)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callbackFailure(e)
            }
        }
    }
}

public fun <T> toCallbackFunction(fn: suspend () -> T): (
    callbackSuccess: (T) -> Unit,
    callbackFailure: (Exception) -> Unit,
) -> Unit = { callbackSuccess, callbackFailure ->
    awaitSuspend(fn, callbackSuccess, callbackFailure)
}

public fun <A, T> toCallbackFunction(fn: suspend (A) -> T): (
    a: A,
    callbackSuccess: (T) -> Unit,
    callbackFailure: (Exception) -> Unit
) -> Unit = { a, callbackSuccess, callbackFailure ->
    awaitSuspend(suspend { fn(a) }, callbackSuccess, callbackFailure)
}

public fun <A, B, T> toCallbackFunction(fn: suspend (A, B) -> T): (
    a: A,
    b: B,
    callbackSuccess: (T) -> Unit,
    callbackFailure:(Exception) -> Unit,
) -> Unit = { a, b, callbackSuccess, callbackFailure ->
    awaitSuspend(suspend { fn(a, b) }, callbackSuccess, callbackFailure)
}

public fun <A, B, C, T> toCallbackFunction(fn: suspend (A, B, C) -> T): (
    a: A,
    b: B,
    c: C,
    callbackSuccess: (T) -> Unit,
    callbackFailure: (Exception) ->Unit
) -> Unit = { a, b, c, callbackSuccess, callbackFailure ->
    awaitSuspend(suspend { fn(a, b, c) }, callbackSuccess, callbackFailure)
}
