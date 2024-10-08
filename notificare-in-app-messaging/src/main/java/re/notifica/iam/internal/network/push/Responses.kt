package re.notifica.iam.internal.network.push

import com.squareup.moshi.JsonClass
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.utilities.moshi.UseDefaultsWhenNull

@JsonClass(generateAdapter = true)
internal data class InAppMessageResponse(
    val message: Message,
) {

    @UseDefaultsWhenNull
    @JsonClass(generateAdapter = true)
    data class Message(
        val _id: String,
        val name: String,
        val type: String,
        val context: List<String> = listOf(),
        val title: String?,
        val message: String?,
        val image: String?,
        val landscapeImage: String?,
        val delaySeconds: Int = 0,
        val primaryAction: Action?,
        val secondaryAction: Action?,
    ) {

        @UseDefaultsWhenNull
        @JsonClass(generateAdapter = true)
        data class Action(
            val label: String?,
            val destructive: Boolean = false,
            val url: String?,
        )

        fun toModel(): NotificareInAppMessage {
            return NotificareInAppMessage(
                id = _id,
                name = name,
                type = type,
                context = context,
                title = title,
                message = message,
                image = image,
                landscapeImage = landscapeImage,
                delaySeconds = delaySeconds,
                primaryAction = primaryAction?.let {
                    NotificareInAppMessage.Action(
                        label = it.label,
                        destructive = it.destructive,
                        url = it.url,
                    )
                },
                secondaryAction = secondaryAction?.let {
                    NotificareInAppMessage.Action(
                        label = it.label,
                        destructive = it.destructive,
                        url = it.url,
                    )
                },
            )
        }
    }
}
