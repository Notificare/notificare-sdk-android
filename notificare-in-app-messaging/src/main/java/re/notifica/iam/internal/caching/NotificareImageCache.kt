package re.notifica.iam.internal.caching

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import re.notifica.internal.NotificareLogger

internal object NotificareImageCache {
    private var image: Bitmap? = null
    private var landscapeImage: Bitmap? = null

    fun getOrientationConstrainedImage(context: Context): Bitmap? {
        return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.landscapeImage ?: this.image
        } else {
            this.image ?: this.landscapeImage
        }
    }

    internal suspend fun loadImages(image: String?, landscapeImage: String?, context: Context) {
        if(!image.isNullOrBlank()) {
            NotificareImageCache.image = loadImage(image, context)
        }
        if (!landscapeImage.isNullOrBlank()) {
            NotificareImageCache.landscapeImage = loadImage(landscapeImage, context)
        }
    }

    private suspend fun loadImage(url: String, context: Context): Bitmap? {
        return try {
            val futureTarget: FutureTarget<Bitmap> = Glide.with(context).asBitmap().load(url).submit()
            withContext(Dispatchers.IO) {
                futureTarget.get()
            }
        } catch (e: Exception) {
            NotificareLogger.warning("Failed to load image from $url", e)
            null
        }
    }
}
