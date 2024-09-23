package re.notifica.push.ui.notifications.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.utilities.logging.NotificareLogger
import re.notifica.utilities.parcel.parcelable
import re.notifica.push.ui.R
import re.notifica.push.ui.actions.NotificationCallbackAction
import re.notifica.push.ui.models.NotificarePendingResult
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

public class NotificareCallbackActionFragment private constructor() : Fragment() {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "NotificareCallbackActionFragment"
    )

    private lateinit var pendingResult: NotificarePendingResult
    private lateinit var callback: NotificationFragment.Callback

    // UI references
    private var imageView: ImageView? = null
    private var messageEditText: EditText? = null
    private var sendButton: ImageButton? = null

    //
    private var imageBytes: ByteArray? = null
    private var mimeType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            callback = parentFragment as NotificationFragment.Callback
        } catch (e: ClassCastException) {
            throw ClassCastException("Parent fragment must implement NotificationFragment.Callback.")
        }

        pendingResult = savedInstanceState?.parcelable(EXTRA_PENDING_RESULT)
            ?: arguments?.parcelable(EXTRA_PENDING_RESULT)
            ?: throw IllegalArgumentException("Missing required pending result parameter.")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        callback.onNotificationFragmentShouldShowActionBar()

        val viewResourceId = when (pendingResult.requestCode) {
            NotificarePendingResult.CAPTURE_IMAGE_AND_KEYBOARD_REQUEST_CODE -> R.layout.notificare_action_callback_camera_keyboard
            NotificarePendingResult.CAPTURE_IMAGE_REQUEST_CODE -> R.layout.notificare_action_callback_camera
            NotificarePendingResult.KEYBOARD_REQUEST_CODE -> R.layout.notificare_action_callback_keyboard
            else -> throw IllegalArgumentException("Unhandled request code '${pendingResult.requestCode}'.")
        }

        return inflater.inflate(viewResourceId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.notificare_callback_image)
        messageEditText = view.findViewById(R.id.notificare_callback_message)
        sendButton = view.findViewById(R.id.notificare_send_button)

        if (imageView != null) {
            val success = renderImage()
            if (!success) {
                // Image capture failed, advise user
                callback.onNotificationFragmentFinished()
            }
        }

        if (messageEditText != null) {
            messageEditText?.requestFocus()
            activity?.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }

        sendButton?.setOnClickListener(::onSendClicked)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_PENDING_RESULT, pendingResult)
    }

    private fun renderImage(): Boolean {
        val imageView = imageView ?: return false
        val imageUri = pendingResult.imageUri ?: return false

        val applicationContext = requireContext().applicationContext
        val mediaStorageDir = requireNotNull(applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        val imagePath = mediaStorageDir.toString() + File.separator + imageUri.lastPathSegment

        // Read bitmap bounds, calculate sample size
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)

        // Read bitmap, convert to JPEG
        options.inSampleSize = calculateSampleSize(options)
        options.inJustDecodeBounds = false

        val srcBitmap = BitmapFactory.decodeFile(imagePath, options) ?: return false

        val matrix = Matrix()
        matrix.setRotate(getImageOrientation(imagePath).toFloat())

        val output = ByteArrayOutputStream()
        val dstBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, false)
        dstBitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)

        imageView.setImageBitmap(dstBitmap)

        imageBytes = output.toByteArray()
        mimeType = "image/jpeg"

        return true
    }

    private fun onSendClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        callback.onNotificationFragmentStartProgress()

        lifecycleScope.launch {
            try {
                val mediaUrl = imageBytes?.let {
                    Notificare.uploadNotificationReplyAsset(
                        payload = it,
                        contentType = requireNotNull(mimeType)
                    )
                }

                NotificationCallbackAction.send(
                    notification = pendingResult.notification,
                    action = pendingResult.action,
                    message = messageEditText?.text?.toString(),
                    mediaUrl = mediaUrl,
                    mimeType = mimeType,
                )

                logger.debug("Created a notification reply.")
                callback.onNotificationFragmentEndProgress()
                callback.onNotificationFragmentActionSucceeded()
                callback.onNotificationFragmentFinished()
            } catch (e: Exception) {
                logger.error("Failed to create a notification reply.", e)
                callback.onNotificationFragmentEndProgress()
                callback.onNotificationFragmentActionFailed(getString(R.string.notificare_action_failed))
                callback.onNotificationFragmentFinished()
            }
        }
    }

    public companion object {
        private const val EXTRA_PENDING_RESULT = "re.notifica.extra.PendingResult"

        private const val SAMPLE_SIZE_MAX_PIXELS = 307200 // 640 x 480

        private val logger = NotificareLogger(
            Notificare.options?.debugLoggingEnabled ?: false,
            "NotificareCallbackActionFragment\$Companion"
        )

        public fun newInstance(pendingResult: NotificarePendingResult): NotificareCallbackActionFragment {
            return NotificareCallbackActionFragment().apply {
                arguments = bundleOf(
                    EXTRA_PENDING_RESULT to pendingResult
                )
            }
        }

        /**
         * Calculate sample size for images so generated bitmap will be smaller than SAMPLE_SIZE_MAX_PIXELS pixels
         *
         * @return the sample factor
         */
        private fun calculateSampleSize(options: BitmapFactory.Options): Int {
            val pixels = options.outHeight * options.outWidth
            var sampleSize = 1
            while (pixels / (sampleSize * sampleSize) > SAMPLE_SIZE_MAX_PIXELS) {
                sampleSize *= 2
            }

            logger.debug(
                "Reading bitmap image of ${options.outWidth}x${options.outHeight} pixels with sampleSize $sampleSize"
            )
            return sampleSize
        }

        /**
         * Return the orientation of the image from the EXIF
         *
         * @param imagePath the path of the image
         * @return the orientation of the image in degrees
         */
        private fun getImageOrientation(imagePath: String): Int {
            return try {
                val exif = ExifInterface(imagePath)

                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    else -> 0
                }
            } catch (e: IOException) {
                logger.error("Couldn't read image file.", e)
                0
            }
        }
    }
}
