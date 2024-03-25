package re.notifica.geo.hms.internal.ktx

import com.huawei.hmf.tasks.CancellationTokenSource
import com.huawei.hmf.tasks.Task
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

@Suppress("DeferredIsResult")
internal fun <T> Task<T>.asDeferred(
    cancellationTokenSource: CancellationTokenSource? = null
): Deferred<T> {
    val deferred = CompletableDeferred<T>()
    if (isComplete) {
        val e = exception
        if (e == null) {
            if (isCanceled) {
                deferred.cancel()
            } else {
                deferred.complete(result)
            }
        } else {
            deferred.completeExceptionally(e)
        }
    } else {
        addOnCompleteListener {
            val e = it.exception
            if (e == null) {
                if (it.isCanceled) {
                    deferred.cancel()
                } else {
                    deferred.complete(it.result)
                }
            } else {
                deferred.completeExceptionally(e)
            }
        }
    }

    if (cancellationTokenSource != null) {
        deferred.invokeOnCompletion {
            cancellationTokenSource.cancel()
        }
    }

    // Prevent casting to CompletableDeferred and manual completion.
    return object : Deferred<T> by deferred {}
}
