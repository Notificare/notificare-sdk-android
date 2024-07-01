package re.notifica.push.ui.utils

import android.net.Uri

internal fun Uri.Builder.removeQueryParameter(key: String): Uri.Builder {
    val uri = this.build()

    this.clearQuery()

    for (param in uri.queryParameterNames) {
        if (param == key) continue

        val value = uri.getQueryParameter(param)
        this.appendQueryParameter(param, value)
    }

    return this
}
