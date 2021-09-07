package re.notifica.push.ui.actions

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.R
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.models.NotificarePendingResult
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

internal class NotificationCallbackAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute(): NotificarePendingResult? = withContext(Dispatchers.IO) {
        when {
            action.camera -> {
                // TODO check image vs video

                val imageUri = getOutputMediaFileUri(MediaType.IMAGE)
                    ?: throw IllegalStateException(context.getString(R.string.notificare_action_camera_failed)) // Cannot save file.

                val requestCode =
                    if (action.keyboard) NotificarePendingResult.CAPTURE_IMAGE_AND_KEYBOARD_REQUEST_CODE
                    else NotificarePendingResult.CAPTURE_IMAGE_REQUEST_CODE

                NotificarePendingResult(
                    notification = notification,
                    action = action,
                    requestCode = requestCode,
                    imageUri = imageUri,
                )
            }
            action.keyboard -> {
                // Just Keyboard, return the result to the caller.
                NotificarePendingResult(
                    notification = notification,
                    action = action,
                    requestCode = NotificarePendingResult.KEYBOARD_REQUEST_CODE,
                    imageUri = null,
                )
            }
            else -> {
                // Just do the call.
                send(notification, action, null, null, null)

                null
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
                NotificarePushUI.contentFileProviderAuthority,
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

    companion object {
        suspend fun send(
            notification: NotificareNotification,
            action: NotificareNotification.Action,
            message: String?,
            mediaUrl: String?,
            mimeType: String?
        ): Unit = withContext(Dispatchers.IO) {
            val targetUri = action.target?.let { Uri.parse(it) }
            if (targetUri == null || targetUri.scheme == null || targetUri.host == null) {
                Notificare.createNotificationReply(
                    notification = notification,
                    action = action,
                    message = message,
                    media = mediaUrl,
                    mimeType = mimeType
                )

                NotificarePushUI.lifecycleListeners.forEach { it.onActionExecuted(notification, action) }

                return@withContext
            }

            val params = mutableMapOf<String, String>()
            params["notificationID"] = notification.id
            params["label"] = action.label
            message?.let { params["message"] = it }
            mediaUrl?.let { params["media"] = it }
            mimeType?.let { params["mimeType"] = it }

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    Notificare.callNotificationReplyWebhook(targetUri, params)
                    NotificarePushUI.lifecycleListeners.forEach { it.onActionExecuted(notification, action) }
                } catch (e: Exception) {
                    NotificarePushUI.lifecycleListeners.forEach { it.onActionFailedToExecute(notification, action, e) }
                }
            }

            Notificare.createNotificationReply(
                notification = notification,
                action = action,
                message = message,
                media = mediaUrl,
                mimeType = mimeType
            )
        }
    }

    enum class MediaType {
        IMAGE,
        VIDEO,
    }
}
