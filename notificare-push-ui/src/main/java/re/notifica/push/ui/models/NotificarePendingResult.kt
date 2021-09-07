package re.notifica.push.ui.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import re.notifica.models.NotificareNotification

@Parcelize
public data class NotificarePendingResult(
    val notification: NotificareNotification,
    val action: NotificareNotification.Action,
    val requestCode: Int?,
    val imageUri: Uri?,
) : Parcelable {

    public companion object {
        public const val CAPTURE_IMAGE_REQUEST_CODE: Int = 100
        public const val CAPTURE_IMAGE_AND_KEYBOARD_REQUEST_CODE: Int = 200
        public const val KEYBOARD_REQUEST_CODE: Int = 300
    }
}
