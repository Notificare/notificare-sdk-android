package re.notifica.loyalty

import android.app.Activity
import androidx.lifecycle.LiveData
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.loyalty.ktx.INTENT_ACTION_PASSBOOK_OPENED
import re.notifica.loyalty.ktx.INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED
import re.notifica.loyalty.ktx.INTENT_EXTRA_PASSBOOK
import re.notifica.loyalty.ktx.loyalty
import re.notifica.loyalty.models.NotificarePass

public object NotificareLoyaltyCompat {

    // region Intent actions

    @JvmField
    public val INTENT_ACTION_PASSBOOK_OPENED: String = Notificare.INTENT_ACTION_PASSBOOK_OPENED

    @JvmField
    public val INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED: String =
        Notificare.INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED

    // endregion

    // region Intent extras

    @JvmField
    public val INTENT_EXTRA_PASSBOOK: String = Notificare.INTENT_EXTRA_PASSBOOK

    // endregion

    @JvmStatic
    public var passbookActivity: Class<out PassbookActivity>
        get() = Notificare.loyalty().passbookActivity
        set(value) {
            Notificare.loyalty().passbookActivity = value
        }

    @JvmStatic
    public val passes: List<NotificarePass>
        get() = Notificare.loyalty().passes

    @JvmStatic
    public val observablePasses: LiveData<List<NotificarePass>> =
        Notificare.loyalty().observablePasses

    @JvmStatic
    public fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>) {
        Notificare.loyalty().fetchPassBySerial(serial, callback)
    }

    @JvmStatic
    public fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>) {
        Notificare.loyalty().fetchPassByBarcode(barcode, callback)
    }

    @JvmStatic
    public fun isInWallet(pass: NotificarePass): Boolean {
        return Notificare.loyalty().isInWallet(pass)
    }

    @JvmStatic
    public fun addPass(pass: NotificarePass, callback: NotificareCallback<Unit>) {
        Notificare.loyalty().addPass(pass, callback)
    }

    @JvmStatic
    public fun removePass(pass: NotificarePass, callback: NotificareCallback<Unit>) {
        Notificare.loyalty().removePass(pass, callback)
    }

    @JvmStatic
    public fun present(activity: Activity, pass: NotificarePass) {
        Notificare.loyalty().present(activity, pass)
    }
}
