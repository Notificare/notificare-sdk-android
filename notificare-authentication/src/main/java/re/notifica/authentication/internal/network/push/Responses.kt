package re.notifica.authentication.internal.network.push

import com.squareup.moshi.JsonClass
import re.notifica.authentication.models.NotificareUser
import re.notifica.authentication.models.NotificareUserSegment
import java.util.*

@JsonClass(generateAdapter = true)
internal data class OAuthResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
)

@JsonClass(generateAdapter = true)
internal data class UserDetailsResponse(
    val user: User,
) {

    @JsonClass(generateAdapter = true)
    internal data class User(
        val _id: String,
        val userName: String,
        val accessToken: String?,
        val segments: List<String>,
        val registrationDate: Date,
        val lastActive: Date,
    ) {

        internal fun toModel(): NotificareUser {
            return NotificareUser(
                id = _id,
                name = userName,
                pushEmailAddress = accessToken?.let { accessToken ->
                    "$accessToken@pushmail.notifica.re"
                },
                segments = segments,
                registrationDate = registrationDate,
                lastActive = lastActive,
            )
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class FetchUserPreferencesResponse(
    val userPreferences: List<Preference>,
) {

    @JsonClass(generateAdapter = true)
    internal data class Preference(
        val _id: String,
        val label: String,
        val preferenceType: String,
        val preferenceOptions: List<PreferenceOption>,
        val indexPosition: Int,
    )

    @JsonClass(generateAdapter = true)
    internal data class PreferenceOption(
        val userSegment: String,
        val label: String,
    )
}

@JsonClass(generateAdapter = true)
internal data class FetchUserSegmentsResponse(
    val userSegments: List<Segment>,
) {

    @JsonClass(generateAdapter = true)
    internal data class Segment(
        val _id: String,
        val name: String,
        val description: String?,
    ) {

        internal fun toModel(): NotificareUserSegment {
            return NotificareUserSegment(
                id = _id,
                name = name,
                description = description,
            )
        }
    }
}
