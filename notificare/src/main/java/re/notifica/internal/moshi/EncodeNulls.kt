package re.notifica.internal.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.rawType
import re.notifica.InternalNotificareApi
import java.lang.reflect.Type

@InternalNotificareApi
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class EncodeNulls

internal class EncodeNullsFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val rawType = type.rawType
        if (!rawType.isAnnotationPresent(EncodeNulls::class.java)) {
            return null
        }

        val delegate: JsonAdapter<Any> = moshi.nextAdapter(this, type, annotations)
        return delegate.serializeNulls()
    }
}
