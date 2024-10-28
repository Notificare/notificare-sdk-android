package re.notifica.loyalty

import android.app.Activity
import re.notifica.NotificareCallback
import re.notifica.loyalty.models.NotificarePass

public interface NotificareLoyalty {

    /**
     * Specifies the class responsible for displaying a Passbook-style activity.
     *
     * This property must be set to a class that extends [PassbookActivity], which is used to present
     * pass details to the user. When invoking the `present` method, this class will be used to handle
     * the UI and interaction for displaying the pass.
     */
    public var passbookActivity: Class<out PassbookActivity>

    /**
     * Fetches a pass by its serial number.
     *
     * @param serial The serial number of the pass to be fetched.
     * @return The fetched [NotificarePass] corresponding to the given serial number.
     *
     * @see [NotificarePass]
     */
    public suspend fun fetchPassBySerial(serial: String): NotificarePass

    /**
     * Fetches a pass by its serial number with a callback.
     *
     * @param serial The serial number of the pass to be fetched.
     * @param callback A callback that will be invoked with the result of the fetch operation.
     *                 This will provide either the [NotificarePass] on success
     *                 or an error on failure.
     */
    public fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>)

    /**
     * Fetches a pass by its barcode.
     *
     * @param barcode The barcode of the pass to be fetched.
     * @return The fetched [NotificarePass] corresponding to the given barcode.
     *
     * @see [NotificarePass]
     */
    public suspend fun fetchPassByBarcode(barcode: String): NotificarePass

    /**
     * Fetches a pass by its barcode with a callback.
     *
     * @param barcode The barcode of the pass to be fetched.
     * @param callback A callback that will be invoked with the result of the fetch operation.
     *                 This will provide either the [NotificarePass] on success
     *                 or an error on failure.
     */
    public fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>)

    /**
     * Presents a pass to the user in a Passbook-style activity.
     *
     * This method uses the activity specified in [passbookActivity] to present the given pass to the user.
     *
     * @param activity The [Activity] context from which the pass presentation will be launched.
     * @param pass The [NotificarePass] to be presented to the user.
     */
    public fun present(activity: Activity, pass: NotificarePass)
}
