package re.notifica.iam.internal.ktx

import android.content.Context
import android.content.res.Configuration
import re.notifica.iam.models.NotificareInAppMessage

/**
 * Returns the most appropriate image considering the current orientation.
 * Falls back to others images when no image was provided for the current orientation.
 */
internal fun NotificareInAppMessage.getOrientationConstrainedImage(context: Context): String? {
    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        return this.landscapeImage ?: this.image
    }

    return this.image ?: this.landscapeImage
}
