package re.notifica.authentication.internal.oauth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Credentials(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
