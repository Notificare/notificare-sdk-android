package re.notifica.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public suspend fun loadBitmap(context: Context, url: String): Bitmap = suspendCancellableCoroutine { continuation ->
    Glide.with(context)
        .asBitmap()
        .load(url)
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (continuation.isActive) {
                    continuation.resume(resource)
                }
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                if (continuation.isActive) {
                    continuation.resumeWithException(RuntimeException("Failed to load the bit at $url"))
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // no-op
            }
        })
}

public fun loadImage(context: Context, url: String, view: ImageView) {
    Glide.with(context)
        .load(url)
        .into(view)
}
