package re.notifica.sample.user.inbox.network

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Url

internal interface UserInboxService {
    @GET
    suspend fun fetchInbox(
        @Header("Authorization") authHeader: String,
        @Url url: String
    ): String

    @PUT
    suspend fun registerDeviceWithUser(
        @Header("Authorization") authHeader: String,
        @Url url: String
    )

    @DELETE
    suspend fun registerDeviceAsAnonymous(
        @Header("Authorization") authHeader: String,
        @Url url: String
    )
}
