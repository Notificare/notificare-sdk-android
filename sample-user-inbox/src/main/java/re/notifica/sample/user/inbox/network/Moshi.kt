package re.notifica.sample.user.inbox.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.util.Date

internal val moshi: Moshi by lazy {
    val builder = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter())

    return@lazy builder.build()
}
