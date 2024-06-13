package re.notifica.iam.internal

import android.graphics.Bitmap
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import re.notifica.internal.NotificareLogger

internal object ImagePreloader {
    public var image: Bitmap? = null
    public var landscapeImage: Bitmap? = null

    suspend fun loadImages(image: String?, landscapeImage: String?, glide: RequestManager) = coroutineScope {
        val imageJob = async { loadImage(image, glide) }
        val landscapeImageJob = async { loadImage(landscapeImage, glide) }
        this@ImagePreloader.image = imageJob.await()
        this@ImagePreloader.landscapeImage = landscapeImageJob.await()
    }

    private suspend fun loadImage(url: String?, glide: RequestManager): Bitmap? {
        return if (url != null) {
            try {
                val futureTarget: FutureTarget<Bitmap> = glide.asBitmap().load(url).submit()
                withContext(Dispatchers.IO) { futureTarget.get() }
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to load image from $url", e)
                null
            }
        } else {
            null
        }
    }
}
