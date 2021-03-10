package re.notifica.push.ui.actions

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.actions.base.NotificationAction
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class NotificationCallbackAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute() {
        if (action.camera) {
            if (context is Activity) {

            } else {
                // TODO cannot launch camera activity from non-activity context
                // cannot launch camera activity from non-activity context
                // callback.onError(NotificareError(R.string.notificare_action_camera_failed))
            }
        }
    }

    /**
     * Create a file Uri for saving an image or video
     * @param type the type, either [MediaType.IMAGE] or [MediaType.VIDEO]
     * @return a ContentProvider URI
     */
    private fun getOutputMediaFileUri(type: MediaType): Uri? {
        try {
            val file = getOutputMediaFile(type) ?: return null
            return FileProvider.getUriForFile(
                Notificare.requireContext(),
                NotificarePushUI.CONTENT_FILE_PROVIDER_AUTHORITY_SUFFIX,
                file
            )
        } catch (e: Exception) {
            NotificareLogger.warning("Failed to create image file.", e)
            return null
        }
    }

    /**
     * Create a File for saving an image or video
     * @param type the type, either [MediaType.IMAGE] or [MediaType.VIDEO]
     * @return a File location
     */
    @Throws(IOException::class)
    private fun getOutputMediaFile(type: MediaType): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (Environment.getExternalStorageState() == null) {
            NotificareLogger.warning("Failed to access external storage.")
            return null
        }

        val mediaStorageDir = Notificare.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return when (type) {
            MediaType.IMAGE -> File.createTempFile("IMG_$timeStamp", ".jpg", mediaStorageDir)
            MediaType.VIDEO -> File.createTempFile("VID_$timeStamp", ".mp4", mediaStorageDir)
        }.also {
            NotificareLogger.debug("Saving file to '${it.absolutePath}'.")
        }
    }

    enum class MediaType {
        IMAGE,
        VIDEO,
    }
}
