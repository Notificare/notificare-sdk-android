package re.notifica.internal.network

import okhttp3.*
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger

internal class NotificareBasicAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request {
        val key = Notificare.applicationKey
        val secret = Notificare.applicationSecret

        if (key == null || secret == null) {
            NotificareLogger.warning("Performing unauthenticated request.")
            return response.request
        }

        val credential = Credentials.basic(key, secret)
        return response.request.newBuilder()
            .header("Authorization", credential)
            .build()
    }
}
