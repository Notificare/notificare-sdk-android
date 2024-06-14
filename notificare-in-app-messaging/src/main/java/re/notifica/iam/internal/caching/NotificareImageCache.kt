package re.notifica.iam.internal.caching

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.iam.models.NotificareInAppMessage

internal object NotificareImageCache {
    private var portraitImage: Bitmap? = null
    private var landscapeImage: Bitmap? = null

    internal var isLoading: Boolean = false
        private set

    internal fun getOrientationConstrainedImage(context: Context): Bitmap? {
        return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.landscapeImage ?: this.portraitImage
        } else {
            this.portraitImage ?: this.landscapeImage
        }
    }

    internal suspend fun preloadImages(
        context: Context,
        message: NotificareInAppMessage,
    ): Unit = withContext(Dispatchers.IO) {
        clear()

        try {
            isLoading = true

            if (!message.image.isNullOrBlank()) {
                portraitImage = loadImage(context, Uri.parse(message.image))
            }

            if (!message.landscapeImage.isNullOrBlank()) {
                landscapeImage = loadImage(context, Uri.parse(message.landscapeImage))
            }
        } finally {
            isLoading = false
        }
    }

    internal fun clear() {
        portraitImage = null
        landscapeImage = null
    }

    private suspend fun loadImage(
        context: Context,
        uri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .submit()
            .get()
    }
}
