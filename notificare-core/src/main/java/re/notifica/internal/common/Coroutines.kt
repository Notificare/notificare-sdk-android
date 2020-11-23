package re.notifica.internal.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import re.notifica.NotificareCallback

internal fun <T> runBlockingNotificare(
    callback: NotificareCallback<T>,
    action: suspend () -> T,
) = runBlocking(Dispatchers.IO) {
    try {
        val result = action()
        callback.onSuccess(result)
    } catch (e: Exception) {
        callback.onFailure(e)
    }
}
