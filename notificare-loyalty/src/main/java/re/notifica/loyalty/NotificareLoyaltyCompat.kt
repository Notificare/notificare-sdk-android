package re.notifica.loyalty

import android.app.Activity
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.loyalty.ktx.INTENT_ACTION_PASSBOOK_OPENED
import re.notifica.loyalty.ktx.INTENT_EXTRA_PASSBOOK
import re.notifica.loyalty.ktx.loyalty
import re.notifica.loyalty.models.NotificarePass

public object NotificareLoyaltyCompat {

    // region Intent actions

    @JvmField
    public val INTENT_ACTION_PASSBOOK_OPENED: String = Notificare.INTENT_ACTION_PASSBOOK_OPENED

    // endregion

    // region Intent extras

    @JvmField
    public val INTENT_EXTRA_PASSBOOK: String = Notificare.INTENT_EXTRA_PASSBOOK

    // endregion

    /**
     * Specifies the class responsible for displaying a Passbook-style activity.
     *
     * This property must be set to a class that extends [PassbookActivity], which is used to present
     * pass details to the user. When invoking the `present` method, this class will be used to handle
     * the UI and interaction for displaying the pass.
     */
    @JvmStatic
    public var passbookActivity: Class<out PassbookActivity>
        get() = Notificare.loyalty().passbookActivity
        set(value) {
            Notificare.loyalty().passbookActivity = value
        }

    /**
     * Fetches a pass by its serial number with a callback.
     *
     * @param serial The serial number of the pass to be fetched.
     * @param callback A callback that will be invoked with the result of the fetch operation.
     *                 This will provide either the [NotificarePass] on success
     *                 or an error on failure.
     */
    @JvmStatic
    public fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>) {
        Notificare.loyalty().fetchPassBySerial(serial, callback)
    }

    /**
     * Fetches a pass by its barcode with a callback.
     *
     * @param barcode The barcode of the pass to be fetched.
     * @param callback A callback that will be invoked with the result of the fetch operation.
     *                 This will provide either the [NotificarePass] on success
     *                 or an error on failure.
     */
    @JvmStatic
    public fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>) {
        Notificare.loyalty().fetchPassByBarcode(barcode, callback)
    }

    /**
     * Presents a pass to the user in a Passbook-style activity.
     *
     * This method uses the activity specified in [passbookActivity] to present the given pass to the user.
     *
     * @param activity The [Activity] context from which the pass presentation will be launched.
     * @param pass The [NotificarePass] to be presented to the user.
     */
    @JvmStatic
    public fun present(activity: Activity, pass: NotificarePass) {
        Notificare.loyalty().present(activity, pass)
    }
}
