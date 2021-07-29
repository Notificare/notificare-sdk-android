package re.notifica.authentication.internal.network.push

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CreateAccountPayload(
    val email: String,
    val password: String,
    val name: String?,
)

@JsonClass(generateAdapter = true)
internal data class ChangePasswordPayload(
    val password: String,
)

@JsonClass(generateAdapter = true)
internal data class SendPasswordResetPayload(
    val email: String,
)

@JsonClass(generateAdapter = true)
internal data class ResetPasswordPayload(
    val password: String,
)
