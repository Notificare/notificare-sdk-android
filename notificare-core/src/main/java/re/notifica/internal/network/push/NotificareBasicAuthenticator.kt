package re.notifica.internal.network.push

import okhttp3.*

internal class NotificareBasicAuthenticator(
    private val username: String,
    private val password: String
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val credential = Credentials.basic(username, password)
        return response.request.newBuilder()
            .header("Authorization", credential)
            .build()
    }
}
