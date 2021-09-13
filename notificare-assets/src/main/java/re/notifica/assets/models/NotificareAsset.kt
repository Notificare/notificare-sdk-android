package re.notifica.assets.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import re.notifica.internal.parcelize.NotificareExtraParceler

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareAsset(
    val title: String,
    val description: String?,
    val key: String?,
    val url: String?,
    val button: Button?,
    val metaData: MetaData?,
    val extra: @WriteWith<NotificareExtraParceler> Map<String, Any> = mapOf(),
) : Parcelable {

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Button(
        val label: String?,
        val action: String?,
    ) : Parcelable

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class MetaData(
        val originalFileName: String,
        val contentType: String,
        val contentLength: Int,
    ) : Parcelable
}
