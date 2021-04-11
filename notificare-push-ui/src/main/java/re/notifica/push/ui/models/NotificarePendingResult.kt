package re.notifica.push.ui.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import re.notifica.models.NotificareNotification

@Parcelize
data class NotificarePendingResult(
    val notification: NotificareNotification,
    val action: NotificareNotification.Action,
    val requestCode: Int?,
    val imageUri: Uri?,
) : Parcelable {

    companion object {
        const val CAPTURE_IMAGE_REQUEST_CODE = 100
        const val CAPTURE_IMAGE_AND_KEYBOARD_REQUEST_CODE = 200
        const val KEYBOARD_REQUEST_CODE = 300
    }
}
