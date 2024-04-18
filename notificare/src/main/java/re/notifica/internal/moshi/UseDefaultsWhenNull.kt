package re.notifica.internal.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import re.notifica.InternalNotificareApi

@InternalNotificareApi
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class UseDefaultsWhenNull

internal class UseDefaultsWhenNullFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (!Types.getRawType(type).isAnnotationPresent(UseDefaultsWhenNull::class.java)) {
            return null
        }

        val delegate = moshi.nextAdapter<Any>(this, type, annotations)

        return object : JsonAdapter<Any>() {
            override fun fromJson(reader: JsonReader): Any? {
                @Suppress("UNCHECKED_CAST")
                val blob = reader.readJsonValue() as Map<String, Any?>
                val noNulls = blob.filterValues { it != null }
                return delegate.fromJsonValue(noNulls)
            }

            override fun toJson(writer: JsonWriter, value: Any?) {
                return delegate.toJson(writer, value)
            }
        }
    }
}
