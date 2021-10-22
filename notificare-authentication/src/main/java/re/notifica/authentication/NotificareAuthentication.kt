package re.notifica.authentication

import android.content.Intent
import re.notifica.NotificareCallback
import re.notifica.authentication.models.NotificareUser
import re.notifica.authentication.models.NotificareUserPreference
import re.notifica.authentication.models.NotificareUserSegment

public interface NotificareAuthentication {

    public val isLoggedIn: Boolean

    public suspend fun login(email: String, password: String)

    public fun login(email: String, password: String, callback: NotificareCallback<Unit>)

    public suspend fun logout()

    public fun logout(callback: NotificareCallback<Unit>)

    public suspend fun fetchUserDetails(): NotificareUser

    public fun fetchUserDetails(callback: NotificareCallback<NotificareUser>)

    public suspend fun changePassword(password: String)

    public fun changePassword(password: String, callback: NotificareCallback<Unit>)

    public suspend fun generatePushEmailAddress(): NotificareUser

    public fun generatePushEmailAddress(callback: NotificareCallback<NotificareUser>)

    public suspend fun createAccount(
        email: String,
        password: String,
        name: String? = null,
    )

    public fun createAccount(
        email: String,
        password: String,
        name: String? = null,
        callback: NotificareCallback<Unit>,
    )

    public suspend fun validateUser(token: String)

    public fun validateUser(token: String, callback: NotificareCallback<Unit>)

    public suspend fun sendPasswordReset(email: String)

    public fun sendPasswordReset(email: String, callback: NotificareCallback<Unit>)

    public suspend fun resetPassword(password: String, token: String)

    public fun resetPassword(password: String, token: String, callback: NotificareCallback<Unit>)

    public suspend fun fetchUserPreferences(): List<NotificareUserPreference>

    public fun fetchUserPreferences(callback: NotificareCallback<List<NotificareUserPreference>>)

    public suspend fun fetchUserSegments(): List<NotificareUserSegment>

    public fun fetchUserSegments(callback: NotificareCallback<List<NotificareUserSegment>>)

    public suspend fun addUserSegment(segment: NotificareUserSegment)

    public fun addUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>)

    public suspend fun removeUserSegment(segment: NotificareUserSegment)

    public fun removeUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>)

    public suspend fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
    )

    public fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    )

    public suspend fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
    )

    public fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    )

    public suspend fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
    )

    public fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    )

    public suspend fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
    )

    public fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    )


    public fun parsePasswordResetToken(intent: Intent): String?

    public fun parseValidateUserToken(intent: Intent): String?
}
