package re.notifica.internal.common

import android.os.Handler
import android.os.Looper
import re.notifica.InternalNotificareApi

private val handler: Handler by lazy {
    Handler(Looper.getMainLooper())
}

@InternalNotificareApi
public fun onMainThread(action: () -> Unit) {
    handler.post {
        action()
    }
}
