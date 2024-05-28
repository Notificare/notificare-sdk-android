package re.notifica.push.gms.internal

import androidx.annotation.Keep
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.tasks.await
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.push.models.NotificareTransport
import re.notifica.push.internal.ServiceManager

@Keep
@InternalNotificareApi
public class ServiceManager : ServiceManager() {

    override val available: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override val transport: NotificareTransport
        get() = NotificareTransport.GCM

    override suspend fun getPushToken(): String {
        return Firebase.messaging.token.await()
    }
}
