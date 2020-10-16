package re.notifica.internal.network.push

import re.notifica.internal.network.push.responses.NotificareApplicationResponse
import retrofit2.http.GET

internal interface NotificarePushService {

    @GET("/application/info")
    suspend fun fetchApplication(): NotificareApplicationResponse
}
