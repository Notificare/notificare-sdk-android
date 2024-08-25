package re.notifica.utilities

import android.os.Handler
import android.os.Looper

private val handler: Handler by lazy {
    Handler(Looper.getMainLooper())
}

public fun onMainThread(action: () -> Unit) {
    handler.post {
        action()
    }
}
