package re.notifica.loyalty

import re.notifica.NotificareCallback
import re.notifica.loyalty.models.NotificarePass

public interface NotificareLoyalty {

    public suspend fun fetchPassBySerial(serial: String): NotificarePass

    public fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>)

    public suspend fun fetchPassByBarcode(barcode: String): NotificarePass

    public fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>)
}
