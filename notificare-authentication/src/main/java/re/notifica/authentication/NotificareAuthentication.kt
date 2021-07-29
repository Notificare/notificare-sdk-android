package re.notifica.authentication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import re.notifica.*
import re.notifica.authentication.internal.network.push.*
import re.notifica.authentication.internal.oauth.Credentials
import re.notifica.authentication.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.authentication.models.NotificareUser
import re.notifica.authentication.models.NotificareUserPreference
import re.notifica.authentication.models.NotificareUserSegment
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.modules.NotificareModule

object NotificareAuthentication : NotificareModule() {

    private lateinit var sharedPreferences: NotificareSharedPreferences

    private val authenticationRefreshListener = object : NotificareRequest.AuthenticationRefreshListener {
        override fun onRefreshAuthentication(callback: NotificareCallback<NotificareRequest.Authentication>) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val credentials = sharedPreferences.credentials ?: run {
                        withContext(Dispatchers.Main) {
                            val error = IllegalStateException("Cannot refresh without a previous set of credentials.")
                            callback.onFailure(error)
                        }

                        return@launch
                    }

                    val payload = FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("client_id", requireNotNull(Notificare.applicationKey))
                        .add("client_secret", requireNotNull(Notificare.applicationSecret))
                        .add("refresh_token", credentials.refreshToken)
                        .build()

                    NotificareLogger.debug("Refresh user credentials.")
                    val response = NotificareRequest.Builder()
                        .post("/oauth/token", payload)
                        .responseDecodable(OAuthResponse::class)

                    // Store the credentials.
                    sharedPreferences.credentials = Credentials(
                        accessToken = response.access_token,
                        refreshToken = response.refresh_token,
                        expiresIn = response.expires_in,
                    )

                    withContext(Dispatchers.Main) {
                        callback.onSuccess(NotificareRequest.Authentication.Bearer(response.access_token))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback.onFailure(e)
                    }
                }
            }
        }
    }

    // region Notificare Module

    override fun configure() {
        sharedPreferences = NotificareSharedPreferences(Notificare.requireContext())
    }

    override suspend fun launch() {}

    override suspend fun unlaunch() {}

    // endregion

    val isLoggedIn: Boolean
        get() = sharedPreferences.credentials != null

    suspend fun login(email: String, password: String): Unit = withContext(Dispatchers.IO) {
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

    fun login(email: String, password: String, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = login(email, password)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun logout(): Unit = withContext(Dispatchers.IO) {
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

    fun logout(callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = logout()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun fetchUserDetails(): NotificareUser = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        val user = NotificareRequest.Builder()
            .get("/user/me")
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRefreshListener)
            .responseDecodable(UserDetailsResponse::class)
            .user
            .toModel()

        Notificare.eventsManager.logFetchUserDetails()

        return@withContext user
    }

    fun fetchUserDetails(callback: NotificareCallback<NotificareUser>) {
        GlobalScope.launch {
            try {
                val result = fetchUserDetails()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun changePassword(password: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        val payload = ChangePasswordPayload(
            password = password,
        )

        NotificareRequest.Builder()
            .put("/user/changepassword", payload)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRefreshListener)
            .response()

        Notificare.eventsManager.logChangePassword()
    }

    fun changePassword(password: String, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = changePassword(password)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun generatePushEmailAddress(): NotificareUser = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        val user = NotificareRequest.Builder()
            .put("/user/generatetoken/me", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRefreshListener)
            .responseDecodable(UserDetailsResponse::class)
            .user
            .toModel()

        Notificare.eventsManager.logGeneratePushEmailAddress()

        return@withContext user
    }

    fun generatePushEmailAddress(callback: NotificareCallback<NotificareUser>) {
        GlobalScope.launch {
            try {
                val result = generatePushEmailAddress()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun createAccount(
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

    fun createAccount(email: String, password: String, name: String? = null, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = createAccount(email, password, name)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun validateUser(token: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .put("/user/validate/$token", null)
            .response()

        Notificare.eventsManager.logValidateUser()
    }

    fun validateUser(token: String, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = validateUser(token)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun sendPasswordReset(email: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val payload = SendPasswordResetPayload(
            email = email,
        )

        NotificareRequest.Builder()
            .put("/user/sendpassword", payload)
            .response()

        Notificare.eventsManager.logSendPasswordReset()
    }

    fun sendPasswordReset(email: String, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = sendPasswordReset(email)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun resetPassword(password: String, token: String): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val payload = ResetPasswordPayload(
            password = password,
        )

        NotificareRequest.Builder()
            .put("/user/resetpassword/$token", payload)
            .response()

        Notificare.eventsManager.logSendPasswordReset()
    }

    fun resetPassword(password: String, token: String, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = resetPassword(password, token)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun fetchUserPreferences(): List<NotificareUserPreference> = withContext(Dispatchers.IO) {
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

    fun fetchUserPreferences(callback: NotificareCallback<List<NotificareUserPreference>>) {
        GlobalScope.launch {
            try {
                val result = fetchUserPreferences()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun fetchUserSegments(): List<NotificareUserSegment> = withContext(Dispatchers.IO) {
        checkPrerequisites()

        NotificareRequest.Builder()
            .get("/usersegment/userselectable")
            .responseDecodable(FetchUserSegmentsResponse::class)
            .userSegments
            .map { it.toModel() }
    }

    fun fetchUserSegments(callback: NotificareCallback<List<NotificareUserSegment>>) {
        GlobalScope.launch {
            try {
                val result = fetchUserSegments()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun addUserSegment(segment: NotificareUserSegment): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        NotificareRequest.Builder()
            .put("/user/me/add/${segment.id}", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRefreshListener)
            .response()
    }

    fun addUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = addUserSegment(segment)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun removeUserSegment(segment: NotificareUserSegment): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        checkUserLoggedInPrerequisite()

        val credentials = checkNotNull(sharedPreferences.credentials)

        NotificareRequest.Builder()
            .put("/user/me/remove/${segment.id}", null)
            .authentication(NotificareRequest.Authentication.Bearer(credentials.accessToken))
            .authenticationRefreshListener(authenticationRefreshListener)
            .response()
    }

    fun removeUserSegment(segment: NotificareUserSegment, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                val result = removeUserSegment(segment)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        addUserSegmentToPreference(segment.id, preference)
    }

    fun addUserSegmentToPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>
    ) {
        GlobalScope.launch {
            try {
                val result = addUserSegmentToPreference(segment, preference)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        addUserSegmentToPreference(option.segmentId, preference)
    }

    fun addUserSegmentToPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>
    ) {
        GlobalScope.launch {
            try {
                val result = addUserSegmentToPreference(option, preference)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        removeUserSegmentFromPreference(segment.id, preference)
    }

    fun removeUserSegmentFromPreference(
        segment: NotificareUserSegment,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>
    ) {
        GlobalScope.launch {
            try {
                val result = removeUserSegmentFromPreference(segment, preference)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    suspend fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference
    ): Unit = withContext(Dispatchers.IO) {
        removeUserSegmentFromPreference(option.segmentId, preference)
    }

    fun removeUserSegmentFromPreference(
        option: NotificareUserPreference.Option,
        preference: NotificareUserPreference,
        callback: NotificareCallback<Unit>
    ) {
        GlobalScope.launch {
            try {
                val result = removeUserSegmentFromPreference(option, preference)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
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
            .authenticationRefreshListener(authenticationRefreshListener)
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
            .authenticationRefreshListener(authenticationRefreshListener)
            .response()
    }
}
