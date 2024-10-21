package re.notifica.utilities.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.rawType
import java.lang.reflect.Type

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class EncodeNulls

public class EncodeNullsFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val rawType = type.rawType
        if (!rawType.isAnnotationPresent(EncodeNulls::class.java)) {
            return null
        }

        val delegate: JsonAdapter<Any> = moshi.nextAdapter(this, type, annotations)
        return delegate.serializeNulls()
    }
}
