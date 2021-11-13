package re.notifica.loyalty

import android.app.Activity
import androidx.lifecycle.LiveData
import re.notifica.NotificareCallback
import re.notifica.loyalty.models.NotificarePass

public interface NotificareLoyalty {

    public var passbookActivity: Class<out PassbookActivity>

    public val passes: List<NotificarePass>

    public val observablePasses: LiveData<List<NotificarePass>>

    public suspend fun fetchPassBySerial(serial: String): NotificarePass

    public fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>)

    public suspend fun fetchPassByBarcode(barcode: String): NotificarePass

    public fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>)

    public fun isInWallet(pass: NotificarePass): Boolean

    public suspend fun addPass(pass: NotificarePass)

    public fun addPass(pass: NotificarePass, callback: NotificareCallback<Unit>)

    public suspend fun removePass(pass: NotificarePass)

    public fun removePass(pass: NotificarePass, callback: NotificareCallback<Unit>)

    public fun present(activity: Activity, pass: NotificarePass)
}
