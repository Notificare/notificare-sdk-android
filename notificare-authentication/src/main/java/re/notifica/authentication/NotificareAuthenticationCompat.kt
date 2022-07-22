package re.notifica.authentication

import android.content.Intent
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.authentication.ktx.authentication
import re.notifica.authentication.models.NotificareUser
import re.notifica.authentication.models.NotificareUserPreference
import re.notifica.authentication.models.NotificareUserSegment

public object NotificareAuthenticationCompat {

    @JvmStatic
    public val isLoggedIn: Boolean
        get() = Notificare.authentication().isLoggedIn

    @JvmStatic
    public fun login(email: String, password: String, callback: NotificareCallback<Unit>) {
        Notificare.authentication().login(email, password, callback)
    }

    @JvmStatic
    public fun logout(callback: NotificareCallback<Unit>) {
        Notificare.authentication().logout(callback)
    }

    @JvmStatic
    public fun fetchUserDetails(callback: NotificareCallback<NotificareUser>) {
        Notificare.authentication().fetchUserDetails(callback)
    }

    @JvmStatic
    public fun changePassword(password: String, callback: NotificareCallback<Unit>) {
        Notificare.authentication().changePassword(password, callback)
    }

    @JvmStatic
    public fun generatePushEmailAddress(callback: NotificareCallback<NotificareUser>) {
        Notificare.authentication().generatePushEmailAddress(callback)
    }

    @JvmStatic
    public fun createAccount(
        email: String,
        password: String,
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.authentication().createAccount(
            email = email,
            password = password,
            callback = callback
        )
    }

    @JvmStatic
    public fun createAccount(
        email: String,
        password: String,
        name: String?,
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.authentication().createAccount(email, password, name, callback)
    }

    @JvmStatic
    public fun validateUser(token: String, callback: NotificareCallback<Unit>) {
        Notificare.authentication().validateUser(token, callback)
    }

    @JvmStatic
    public fun sendPasswordReset(email: String, callback: NotificareCallback<Unit>) {
        Notificare.authentication().sendPasswordReset(email, callback)
    }

    @JvmStatic
    public fun resetPassword(password: String, token: String, callback: NotificareCallback<Unit>) {
        Notificare.authentication().resetPassword(password, token, callback)
    }

    @JvmStatic
    public fun fetchUserPreferences(callback: NotificareCallback<List<NotificareUserPreference>>) {
        Notificare.authentication().fetchUserPreferences(callback)
    }

    @JvmStatic
    public fun fetchUserSegments(callback: NotificareCallback<List<NotificareUserSegment>>) {
        Notificare.authentication().fetchUserSegments(callback)
    }

    @JvmStatic
    public fun addUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>) {
        Notificare.authentication().addUserSegment(segment, callback)
    }

    @JvmStatic
    public fun removeUserSegment(
        segment: NotificareUserSegment,
        callback: NotificareCallback<Unit>
    ) {
        Notificare.authentication().removeUserSegment(segment, callback)
    }

    @JvmStatic
    public fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.authentication().addUserSegmentToPreference(segment, preference, callback)
    }

    @JvmStatic
    public fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.authentication().addUserSegmentToPreference(option, preference, callback)
    }

    @JvmStatic
    public fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.authentication().removeUserSegmentFromPreference(segment, preference, callback)
    }

    @JvmStatic
    public fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.authentication().removeUserSegmentFromPreference(option, preference, callback)
    }

    @JvmStatic
    public fun parsePasswordResetToken(intent: Intent): String? {
        return Notificare.authentication().parsePasswordResetToken(intent)
    }

    @JvmStatic
    public fun parseValidateUserToken(intent: Intent): String? {
        return Notificare.authentication().parseValidateUserToken(intent)
    }
}
