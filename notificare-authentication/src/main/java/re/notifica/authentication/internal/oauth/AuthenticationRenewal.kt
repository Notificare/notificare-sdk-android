package re.notifica.authentication.internal.oauth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareLogger
import re.notifica.authentication.NotificareAuthentication
import re.notifica.authentication.internal.network.push.OAuthResponse
import re.notifica.internal.network.request.NotificareRequest

internal class AuthenticationRenewal : NotificareRequest.AuthenticationRefreshListener {

    private val lock = Any()
    private var performingRenewal = false
    private val authenticationCallbacks = mutableListOf<NotificareCallback<NotificareRequest.Authentication>>()

    override fun onRefreshAuthentication(callback: NotificareCallback<NotificareRequest.Authentication>) {
        synchronized(lock) {
            authenticationCallbacks.add(callback)

            if (performingRenewal) {
                NotificareLogger.debug("There is an ongoing refresh process. Adding the callback to the queue.")
                return
            }

            performingRenewal = true
        }

        refreshCredentialsAsync()
    }

    private fun refreshCredentialsAsync() {
        GlobalScope.launch {
            try {
                val credentials = refreshCredentials()
                NotificareAuthentication.sharedPreferences.credentials = credentials

                withContext(Dispatchers.Main) {
                    notify(NotificareRequest.Authentication.Bearer(credentials.accessToken))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    notify(e)
                }
            }
        }
    }

    private suspend fun refreshCredentials(): Credentials = withContext(Dispatchers.IO) {
        val credentials = NotificareAuthentication.sharedPreferences.credentials
            ?: throw IllegalStateException("Cannot refresh without a previous set of credentials.")

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

        Credentials(
            accessToken = response.access_token,
            refreshToken = response.refresh_token,
            expiresIn = response.expires_in,
        )
    }

    private fun notify(authentication: NotificareRequest.Authentication) {
        synchronized(lock) {
            authenticationCallbacks.forEach { it.onSuccess(authentication) }
            authenticationCallbacks.clear()
            performingRenewal = false
        }
    }

    private fun notify(e: Exception) {
        synchronized(lock) {
            authenticationCallbacks.forEach { it.onFailure(e) }
            authenticationCallbacks.clear()
            performingRenewal = false
        }
    }
}
