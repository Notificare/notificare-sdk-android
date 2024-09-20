package re.notifica.sample.user.inbox.ktx

import android.content.res.Resources

val Int.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density
