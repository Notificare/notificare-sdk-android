package re.notifica.internal.moshi

import com.squareup.moshi.*
import org.json.JSONObject

internal class JSONObjectAdapter {

    @ToJson
    fun toJson(writer: JsonWriter, value: JSONObject?, delegate: JsonAdapter<Map<String, Any>>) {
        val decoded = value?.let { delegate.fromJson(it.toString()) }
        delegate.toJson(writer, decoded)
    }

    @FromJson
    fun fromJson(reader: JsonReader, delegate: JsonAdapter<Map<String, Any>>): JSONObject? {
        val decoded = delegate.fromJson(reader)
            ?: return null

        return JSONObject(decoded)
    }
}
