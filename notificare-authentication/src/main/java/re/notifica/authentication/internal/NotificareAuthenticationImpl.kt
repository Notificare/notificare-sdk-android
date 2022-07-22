package re.notifica.authentication.internal

import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.google.api.client.auth.oauth2.GoogleStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import re.notifica.*
import re.notifica.authentication.*
import re.notifica.authentication.internal.network.push.*
import re.notifica.authentication.internal.oauth.AuthenticationRenewal
import re.notifica.authentication.internal.oauth.Credentials
import re.notifica.authentication.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.authentication.ktx.*
import re.notifica.authentication.models.NotificareUser
import re.notifica.authentication.models.NotificareUserPreference
import re.notifica.authentication.models.NotificareUserSegment
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.models.NotificareApplication

@Keep
internal object NotificareAuthenticationImpl : NotificareModule(), NotificareAuthentication {

    internal lateinit var sharedPreferences: NotificareSharedPreferences
    private val authenticationRenewal = AuthenticationRenewal()

    // region Notificare Module

    override fun migrate(savedState: SharedPreferences, settings: SharedPreferences) {
        val storage = GoogleStorageUtils(Notificare.requireContext())
        val storedCredential = storage.loadStoredCredential() ?: return

        val sharedPreferences = NotificareSharedPreferences(Notificare.requireContext())
        sharedPreferences.credentials = Credentials(
            accessToken = storedCredential.accessToken,
            refreshToken = storedCredential.refreshToken,
            expiresIn = storedCredential.expirationTimeMilliseconds,
        )

        storage.removeStoredCredential()
    }

    override fun configure() {
        sharedPreferences = NotificareSharedPreferences(Notificare.requireContext())
    }

    // endregion

    // region Notificare Authentication

    override val isLoggedIn: Boolean
        get() = sharedPreferences.credentials != null

    override suspend fun login(email: String, password: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(Notificare.device().currentDevice)

        val payload = FormBody.Builder()
            .add("grant_type", "password")
            .add("client_id", requireNotNull(Notificare.servicesInfo?.applicationKey))
            .add("client_secret", requireNotNull(Notificare.servicesInfo?.applicationSecret))
            .add("username", email)
            .add("password", password)
            .build()

        NotificareLogger.debug("Logging in the user.")
        val response = NotificareRequest.Builder()
            .post("/oauth/token", payload)
            .responseDecodable(OAuthResponse::class)

        NotificareLogger.debug("Registering the device with the user details.")
        Notificare.device().register(
            userId = email,
            userName = device.userName, // TODO consider fetching the profile and sync the user name in the cached device.
        )

        // Store the credentials.
        sharedPreferences.credentials = Credentials(
            accessToken = response.access_token,
            refreshToken = response.refresh_token,
            expiresIn = response.expires_in,
        )

        Notificare.events().logUserLogin()
    }

    override fun login(email: String, password: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::login)(email, password, callback)

    override suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val device = checkNotNull(Notificare.device().currentDevice)

        NotificareLogger.debug("Removing user from the device.")
        NotificareRequest.Builder()
            .delete("/device/${device.id}/user", null)
            .response()

        NotificareLogger.debug("Removing stored credentials.")
        sharedPreferences.credentials = null

        NotificareLogger.debug("Registering device as anonymous.")
        Notificare.device().register(null, null)

        Notificare.events().logUserLogout()
    }

    override fun logout(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::logout)(callback)

    override suspend fun fetchUserDetails(): NotificareUser = withContext(Dispatchers.IO) {
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

        Notificare.events().logFetchUserDetails()

        return@withContext user
    }

    override fun fetchUserDetails(callback: NotificareCallback<NotificareUser>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::fetchUserDetails)(callback)

    override suspend fun changePassword(password: String): Unit = withContext(Dispatchers.IO) {
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

        Notificare.events().logChangePassword()
    }

    override fun changePassword(password: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::changePassword)(password, callback)

    override suspend fun generatePushEmailAddress(): NotificareUser = withContext(Dispatchers.IO) {
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

        Notificare.events().logGeneratePushEmailAddress()

        return@withContext user
    }

    override fun generatePushEmailAddress(callback: NotificareCallback<NotificareUser>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::generatePushEmailAddress)(callback)

    override suspend fun createAccount(
        email: String,
        password: String,
        name: String?,
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

        Notificare.events().logCreateUserAccount()
    }

    override fun createAccount(
        email: String,
        password: String,
        name: String?,
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(NotificareAuthenticationImpl::createAccount)(email, password, name, callback)

    override suspend fun validateUser(token: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .put("/user/validate/$token", null)
            .response()

        Notificare.events().logValidateUser()
    }

    override fun validateUser(token: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::validateUser)(token, callback)

    override suspend fun sendPasswordReset(email: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val payload = SendPasswordResetPayload(
            email = email,
        )

        NotificareRequest.Builder()
            .put("/user/sendpassword", payload)
            .response()

        Notificare.events().logSendPasswordReset()
    }

    override fun sendPasswordReset(email: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::sendPasswordReset)(email, callback)

    override suspend fun resetPassword(password: String, token: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val payload = ResetPasswordPayload(
            password = password,
        )

        NotificareRequest.Builder()
            .put("/user/resetpassword/$token", payload)
            .response()

        Notificare.events().logResetPassword()
    }

    override fun resetPassword(password: String, token: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::resetPassword)(password, token, callback)

    override suspend fun fetchUserPreferences(): List<NotificareUserPreference> = withContext(Dispatchers.IO) {
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

    override fun fetchUserPreferences(callback: NotificareCallback<List<NotificareUserPreference>>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::fetchUserPreferences)(callback)

    override suspend fun fetchUserSegments(): List<NotificareUserSegment> = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .get("/usersegment/userselectable")
            .responseDecodable(FetchUserSegmentsResponse::class)
            .userSegments
            .map { it.toModel() }
    }

    override fun fetchUserSegments(callback: NotificareCallback<List<NotificareUserSegment>>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::fetchUserSegments)(callback)

    override suspend fun addUserSegment(segment: NotificareUserSegment): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        NotificareRequest.Builder()
            .put("/user/me/add/${segment.id}", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .response()
    }

    override fun addUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::addUserSegment)(segment, callback)

    override suspend fun removeUserSegment(segment: NotificareUserSegment): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        NotificareRequest.Builder()
            .put("/user/me/remove/${segment.id}", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRenewal)
            .response()
    }

    override fun removeUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareAuthenticationImpl::removeUserSegment)(segment, callback)

    override suspend fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
    ): Unit = withContext(Dispatchers.IO) {
        addUserSegmentToPreference(segment.id, preference)
    }

    override fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(suspend { addUserSegmentToPreference(segment, preference) })(callback)

    override suspend fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
    ): Unit = withContext(Dispatchers.IO) {
        addUserSegmentToPreference(option.segmentId, preference)
    }

    override fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(suspend { addUserSegmentToPreference(option, preference) })(callback)

    override suspend fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
    ): Unit = withContext(Dispatchers.IO) {
        removeUserSegmentFromPreference(segment.id, preference)
    }

    override fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(suspend { removeUserSegmentFromPreference(segment, preference) })(callback)

    override suspend fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
    ): Unit = withContext(Dispatchers.IO) {
        removeUserSegmentFromPreference(option.segmentId, preference)
    }

    override fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(suspend { removeUserSegmentFromPreference(option, preference) })(callback)

    override fun parsePasswordResetToken(intent: Intent): String? {
        val uri = intent.data ?: return null
        val pathSegments = uri.pathSegments ?: return null

        val application = Notificare.application ?: return null
        val appLinksDomain = Notificare.servicesInfo?.appLinksDomain ?: return null

        if (uri.host == "${application.id}.${appLinksDomain}" && pathSegments.size >= 3 && pathSegments[0] == "oauth" && pathSegments[1] == "resetpassword") {
            return pathSegments[2]
        }

        return null
    }

    override fun parseValidateUserToken(intent: Intent): String? {
        val uri = intent.data ?: return null
        val pathSegments = uri.pathSegments ?: return null

        val application = Notificare.application ?: return null
        val appLinksDomain = Notificare.servicesInfo?.appLinksDomain ?: return null

        if (uri.host == "${application.id}.${appLinksDomain}" && pathSegments.size >= 3 && pathSegments[0] == "oauth" && pathSegments[1] == "validate") {
            return pathSegments[2]
        }

        return null
    }

    // endregion


    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.OAUTH2] != true) {
            NotificareLogger.warning("Notificare authentication functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.OAUTH2)
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
        preference: NotificareUserPreference,
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
        preference: NotificareUserPreference,
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
