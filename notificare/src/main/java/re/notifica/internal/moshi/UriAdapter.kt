package re.notifica.internal.moshi

import android.net.Uri
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

internal class UriAdapter {
    @ToJson
    fun toJson(uri: Uri): String = uri.toString()

    @FromJson
    fun fromJson(uri: String): Uri = Uri.parse(uri)
}
