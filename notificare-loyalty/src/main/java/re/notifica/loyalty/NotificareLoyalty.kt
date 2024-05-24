package re.notifica.loyalty

import android.app.Activity
import re.notifica.NotificareCallback
import re.notifica.loyalty.models.NotificarePass

public interface NotificareLoyalty {

    public var passbookActivity: Class<out PassbookActivity>

    public suspend fun fetchPassBySerial(serial: String): NotificarePass

    public fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>)

    public suspend fun fetchPassByBarcode(barcode: String): NotificarePass

    public fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>)

    public fun present(activity: Activity, pass: NotificarePass)
}
