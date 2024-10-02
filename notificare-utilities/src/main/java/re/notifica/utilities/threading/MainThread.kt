package re.notifica.utilities.threading

import android.os.Handler
import android.os.Looper

@PublishedApi
internal val mainThreadHandler: Handler by lazy {
    Handler(Looper.getMainLooper())
}

public inline fun onMainThread(crossinline action: () -> Unit) {
    mainThreadHandler.post {
        action()
    }
}
