package re.notifica.authentication

import android.content.Intent
import android.content.SharedPreferences
import com.google.api.client.auth.oauth2.GoogleStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import re.notifica.*
import re.notifica.authentication.internal.network.push.*
import re.notifica.authentication.internal.oauth.AuthenticationRenewal
import re.notifica.authentication.internal.oauth.Credentials
import re.notifica.authentication.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.authentication.models.NotificareUser
import re.notifica.authentication.models.NotificareUserPreference
import re.notifica.authentication.models.NotificareUserSegment
import re.notifica.internal.NotificareLogger
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.modules.NotificareModule

public object NotificareAuthentication : NotificareModule() {

    internal lateinit var sharedPreferences: NotificareSharedPreferences
    private val authenticationRenewal = AuthenticationRenewal()

    // region Notificare Module

    override fun configure() {
        sharedPreferences = NotificareSharedPreferences(Notificare.requireContext())
    }

    override suspend fun launch() {}

    override suspend fun unlaunch() {}

    override fun migrate(savedState: SharedPreferences, settings: SharedPreferences) {
        val storage = GoogleStorageUtils(Notificare.requireContext())
        val storedCredential = storage.loadStoredCredential() ?: return

        sharedPreferences.credentials = Credentials(
            accessToken = storedCredential.accessToken,
            refreshToken = storedCredential.refreshToken,
            expiresIn = storedCredential.expirationTimeMilliseconds,
        )

        storage.removeStoredCredential()
    }

    // endregion

    public val isLoggedIn: Boolean
        get() = sharedPreferences.credentials != null

    public suspend fun login(email: String, password: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(Notificare.deviceManager.currentDevice)

        val payload = FormBody.Builder()
            .add("grant_type", "password")
            .add("client_id", requireNotNull(Notificare.applicationKey))
            .add("client_secret", requireNotNull(Notificare.applicationSecret))
            .add("username", email)
            .add("password", password)
            .build()

        NotificareLogger.debug("Logging in the user.")
        val response = NotificareRequest.Builder()
            .post("/oauth/token", payload)
            .responseDecodable(OAuthResponse::class)

        NotificareLogger.debug("Registering the device with the user details.")
        Notificare.deviceManager.register(
            userId = email,
            userName = device.userName, // TODO consider fetching the profile and sync the user name in the cached device.
        )

        // Store the credentials.
        sharedPreferences.credentials = Credentials(
            accessToken = response.access_token,
            refreshToken = response.refresh_token,
            expiresIn = response.expires_in,
        )

        Notificare.eventsManager.logUserLogin()
    }

    public fun login(email: String, password: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::login)(email, password, callback)

    public suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val device = checkNotNull(Notificare.deviceManager.currentDevice)

        NotificareLogger.debug("Removing user from the device.")
        NotificareRequest.Builder()
            .delete("/device/${device.id}/user", null)
            .response()

        NotificareLogger.debug("Removing stored credentials.")
        sharedPreferences.credentials = null

        NotificareLogger.debug("Registering device as anonymous.")
        Notificare.deviceManager.register(null, null)

        Notificare.eventsManager.logUserLogout()
    }

    public fun logout(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::logout)(callback)

    public suspend fun fetchUserDetails(): NotificareUser = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        val user = NotificareRequest.Builder()
            .get("/user/me")
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .responseDecodable(UserDetailsResponse::class)
            .user
            .toModel()

        Notificare.eventsManager.logFetchUserDetails()

        return@withContext user
    }

    public fun fetchUserDetails(callback: NotificareCallback<NotificareUser>): Unit =
        toCallbackFunction(::fetchUserDetails)(callback)

    public suspend fun changePassword(password: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        val payload = ChangePasswordPayload(
            password = password,
        )

        NotificareRequest.Builder()
            .put("/user/changepassword", payload)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .response()

        Notificare.eventsManager.logChangePassword()
    }

    public fun changePassword(password: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::changePassword)(password, callback)

    public suspend fun generatePushEmailAddress(): NotificareUser = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        val user = NotificareRequest.Builder()
            .put("/user/generatetoken/me", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .responseDecodable(UserDetailsResponse::class)
            .user
            .toModel()

        Notificare.eventsManager.logGeneratePushEmailAddress()

        return@withContext user
    }

    public fun generatePushEmailAddress(callback: NotificareCallback<NotificareUser>): Unit =
        toCallbackFunction(::generatePushEmailAddress)(callback)

    public suspend fun createAccount(
        email: String,
        password: String,
        name: String? = null
    ): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val payload = CreateAccountPayload(
            email = email,
            password = password,
            name = name,
        )

        NotificareRequest.Builder()
            .post("/user", payload)
            .response()

        Notificare.eventsManager.logCreateUserAccount()
    }

    public fun createAccount(
        email: String,
        password: String,
        name: String? = null,
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(::createAccount)(email, password, name, callback)

    public suspend fun validateUser(token: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .put("/user/validate/$token", null)
            .response()

        Notificare.eventsManager.logValidateUser()
    }

    public fun validateUser(token: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::validateUser)(token, callback)

    public suspend fun sendPasswordReset(email: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val payload = SendPasswordResetPayload(
            email = email,
        )

        NotificareRequest.Builder()
            .put("/user/sendpassword", payload)
            .response()

        Notificare.eventsManager.logSendPasswordReset()
    }

    public fun sendPasswordReset(email: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::sendPasswordReset)(email, callback)

    public suspend fun resetPassword(password: String, token: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val payload = ResetPasswordPayload(
            password = password,
        )

        NotificareRequest.Builder()
            .put("/user/resetpassword/$token", payload)
            .response()

        Notificare.eventsManager.logResetPassword()
    }

    public fun resetPassword(password: String, token: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::resetPassword)(password, token, callback)

    public suspend fun fetchUserPreferences(): List<NotificareUserPreference> = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val userDetails = fetchUserDetails()

        NotificareRequest.Builder()
            .get("/userpreference")
            .responseDecodable(FetchUserPreferencesResponse::class)
            .userPreferences
            .map { preference ->
                val type = try {
                    NotificareUserPreference.Type.fromJson(preference.preferenceType)
                } catch (e: Exception) {
                    NotificareLogger.warning("Could not decode preference type '${preference.preferenceType}'.", e)
                    return@map null
                }

                NotificareUserPreference(
                    id = preference._id,
                    label = preference.label,
                    type = type,
                    options = preference.preferenceOptions.map { option ->
                        NotificareUserPreference.Option(
                            label = option.label,
                            segmentId = option.userSegment,
                            selected = userDetails.segments.contains(option.userSegment),
                        )
                    },
                    position = preference.indexPosition,
                )
            }
            .filterNotNull()
    }

    public fun fetchUserPreferences(callback: NotificareCallback<List<NotificareUserPreference>>): Unit =
        toCallbackFunction(::fetchUserPreferences)(callback)

    public suspend fun fetchUserSegments(): List<NotificareUserSegment> = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .get("/usersegment/userselectable")
            .responseDecodable(FetchUserSegmentsResponse::class)
            .userSegments
            .map { it.toModel() }
    }

    public fun fetchUserSegments(callback: NotificareCallback<List<NotificareUserSegment>>): Unit =
        toCallbackFunction(::fetchUserSegments)(callback)

    public suspend fun addUserSegment(segment: NotificareUserSegment): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        NotificareRequest.Builder()
            .put("/user/me/add/${segment.id}", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .response()
    }

    public fun addUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::addUserSegment)(segment, callback)

    public suspend fun removeUserSegment(segment: NotificareUserSegment): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        NotificareRequest.Builder()
            .put("/user/me/remove/${segment.id}", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .response()
    }

    public fun removeUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::removeUserSegment)(segment, callback)

    public suspend fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        addUserSegmentToPreference(segment.id, preference)
    }

    public fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(suspend { addUserSegmentToPreference(segment, preference) })(callback)

    public suspend fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        addUserSegmentToPreference(option.segmentId, preference)
    }

    public fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(suspend { addUserSegmentToPreference(option, preference) })(callback)

    public suspend fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        removeUserSegmentFromPreference(segment.id, preference)
    }

    public fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(suspend { removeUserSegmentFromPreference(segment, preference) })(callback)

    public suspend fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        removeUserSegmentFromPreference(option.segmentId, preference)
    }

    public fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(suspend { removeUserSegmentFromPreference(option, preference) })(callback)

    public fun parsePasswordResetToken(intent: Intent): String? {
        val application = Notificare.application ?: return null
        val uri = intent.data ?: return null
        val host = uri.host ?: return null
        val pathSegments = uri.pathSegments ?: return null

        if (
            !host.startsWith(application.id) ||
            pathSegments.size < 3 ||
            pathSegments[0] != "oauth" ||
            pathSegments[1] != "resetpassword"
        ) return null

        return pathSegments[2]
    }

    public fun parseValidateUserToken(intent: Intent): String? {
        val application = Notificare.application ?: return null
        val uri = intent.data ?: return null
        val host = uri.host ?: return null
        val pathSegments = uri.pathSegments ?: return null

        if (
            !host.startsWith(application.id) ||
            pathSegments.size < 3 ||
            pathSegments[0] != "oauth" ||
            pathSegments[1] != "validate"
        ) return null

        return pathSegments[2]
    }


    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareException.NotReady()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareException.NotReady()
        }

        if (application.services["oauth2"] != true) {
            NotificareLogger.warning("Notificare authentication functionality is not enabled.")
            throw NotificareException.NotReady()
        }
    }

    @Throws
    private fun checkUserLoggedInPrerequisite() {
        if (!isLoggedIn) {
            NotificareLogger.warning("The user is not logged in.")
            throw IllegalStateException("User not logged in.")
        }
    }

    private suspend fun addUserSegmentToPreference(
        segmentId: String,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        if (!preference.options.any { it.segmentId == segmentId }) {
            NotificareLogger.warning("The preference '${preference.label}' does not contain the segment '$segmentId'.")
            throw IllegalArgumentException("The preference '${preference.label}' does not contain the segment '$segmentId'.")
        }

        val credentials = checkNotNull(sharedPreferences.credentials)

        NotificareRequest.Builder()
            .put("/user/me/add/$segmentId/preference/${preference.id}", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .response()
    }

    private suspend fun removeUserSegmentFromPreference(
        segmentId: String,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        if (!preference.options.any { it.segmentId == segmentId }) {
            NotificareLogger.warning("The preference '${preference.label}' does not contain the segment '$segmentId'.")
            throw IllegalArgumentException("The preference '${preference.label}' does not contain the segment '$segmentId'.")
        }

        val credentials = checkNotNull(sharedPreferences.credentials)

        NotificareRequest.Builder()
            .put("/user/me/remove/$segmentId/preference/${preference.id}", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .response()
    }
}
