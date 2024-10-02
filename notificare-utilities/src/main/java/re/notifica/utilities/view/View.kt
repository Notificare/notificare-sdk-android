package re.notifica.utilities.view

import android.view.View
import android.view.ViewTreeObserver

public inline fun View.waitForLayout(crossinline f: () -> Unit) {
    if (!viewTreeObserver.isAlive) return

    if (isLaidOut) return f()

    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (!viewTreeObserver.isAlive) return

            viewTreeObserver.removeOnGlobalLayoutListener(this)
            f()
        }
    })
}
