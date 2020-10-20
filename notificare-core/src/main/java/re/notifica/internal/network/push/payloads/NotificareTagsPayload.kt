package re.notifica.internal.network.push.payloads

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class NotificareTagsPayload(
    val tags: List<String>,
)
