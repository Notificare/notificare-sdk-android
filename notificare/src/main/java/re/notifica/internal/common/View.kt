package re.notifica.internal.common

import android.view.View
import android.view.ViewTreeObserver
import re.notifica.InternalNotificareApi

@InternalNotificareApi
public inline fun View.waitForLayout(crossinline f: () -> Unit) {
    if (!viewTreeObserver.isAlive) return

    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (!viewTreeObserver.isAlive) return

            viewTreeObserver.removeOnGlobalLayoutListener(this)
            f()
        }
    })
}
