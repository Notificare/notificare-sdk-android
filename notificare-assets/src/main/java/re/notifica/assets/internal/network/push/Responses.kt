package re.notifica.assets.internal.network.push

import com.squareup.moshi.JsonClass
import re.notifica.Notificare
import re.notifica.assets.models.NotificareAsset
import re.notifica.internal.moshi.UseDefaultsWhenNull

@JsonClass(generateAdapter = true)
internal data class FetchAssetsResponse(
    val assets: List<Asset>,
) {
    @UseDefaultsWhenNull
    @JsonClass(generateAdapter = true)
    internal data class Asset(
        val title: String,
        val description: String?,
        val key: String?,
        val url: String?,
        val button: Button?,
        val metaData: MetaData?,
        val extra: Map<String, Any> = mapOf(),
    ) {

        internal fun toModel(): NotificareAsset {
            return NotificareAsset(
                title = title,
                description = description,
                key = key,
                url = key?.let { key ->
                    val host = Notificare.servicesInfo?.pushHost ?: return@let null
                    "$host/asset/file/$key"
                },
                button = button?.let {
                    NotificareAsset.Button(
                        label = it.label,
                        action = it.action,
                    )
                },
                metaData = metaData?.let {
                    NotificareAsset.MetaData(
                        originalFileName = it.originalFileName,
                        contentType = it.contentType,
                        contentLength = it.contentLength,
                    )
                },
                extra = extra,
            )
        }

        @JsonClass(generateAdapter = true)
        internal data class Button(
            val label: String?,
            val action: String?,
        )

        @JsonClass(generateAdapter = true)
        internal data class MetaData(
            val originalFileName: String,
            val contentType: String,
            val contentLength: Int,
        )
    }
}
