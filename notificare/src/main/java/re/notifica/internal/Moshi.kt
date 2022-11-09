package re.notifica.internal

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.internal.moshi.EncodeNullsFactory
import re.notifica.internal.moshi.NotificareTimeAdapter
import re.notifica.internal.moshi.UriAdapter
import re.notifica.internal.moshi.UseDefaultsWhenNullFactory
import java.util.*

@InternalNotificareApi
public val Notificare.moshi: Moshi by lazy {
    val builder = Moshi.Builder()
        .add(EncodeNullsFactory())
        .add(UseDefaultsWhenNullFactory())
        .add(Date::class.java, Rfc3339DateJsonAdapter())
        .add(NotificareTimeAdapter())
        .add(UriAdapter())

    NotificareModule.Module.values().forEach { module ->
        module.instance?.moshi(builder)
    }

    return@lazy builder.build()
}
