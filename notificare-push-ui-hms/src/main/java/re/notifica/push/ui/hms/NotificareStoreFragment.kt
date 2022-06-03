package re.notifica.push.ui.hms

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.Keep
import re.notifica.Notificare
import re.notifica.internal.common.onMainThread
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.hms.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment

@Keep
public class NotificareStoreFragment : NotificationFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context = context ?: return

        val referrer = context.packageName
        var uri: Uri? = null

        var content: NotificareNotification.Content? = notification.content.firstOrNull {
            it.type == NotificareNotification.Content.TYPE_APP_GALLERY_DETAILS
        }

        if (content != null) {
            uri = getDetailsIntentUri(content.data as String, referrer)
        } else {
            content = notification.content.firstOrNull {
                it.type == NotificareNotification.Content.TYPE_APP_GALLERY_SEARCH
            }

            if (content != null) {
                uri = getSearchIntentUri(content.data as String, referrer)
            } else {
                content = notification.content.firstOrNull {
                    it.type == NotificareNotification.Content.TYPE_GOOGLE_PLAY_DETAILS
                }

                if (content != null) {
                    uri = getSearchIntentUri(content.data as String, referrer)
                } else {
                    content = notification.content.firstOrNull {
                        it.type == NotificareNotification.Content.TYPE_GOOGLE_PLAY_SEARCH
                    }

                    if (content != null) {
                        uri = getSearchIntentUri(content.data as String, referrer)
                    }
                }
            }
        }

        if (uri != null) {
            try {
                val rateIntent = Intent(Intent.ACTION_VIEW, uri)

                callback.onNotificationFragmentStartActivity(rateIntent)
                callback.onNotificationFragmentFinished()

                onMainThread {
                    Notificare.pushUIInternal().lifecycleListeners.forEach {
                        it.onNotificationPresented(notification)
                    }
                }
            } catch (e: ActivityNotFoundException) {
                callback.onNotificationFragmentActionFailed(resources.getString(R.string.notificare_app_gallery_intent_failed))
                callback.onNotificationFragmentFinished()

                onMainThread {
                    Notificare.pushUIInternal().lifecycleListeners.forEach {
                        it.onNotificationFailedToPresent(notification)
                    }
                }
            }
        } else {
            callback.onNotificationFragmentActionFailed(resources.getString(R.string.notificare_app_gallery_intent_failed))
            callback.onNotificationFragmentFinished()

            onMainThread {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.onNotificationFailedToPresent(notification)
                }
            }
        }
    }

    private fun getDetailsIntentUri(id: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("appmarket")
            .authority("details")
            .appendQueryParameter("id", id)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun getSearchIntentUri(query: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("appmarket")
            .authority("search")
            .appendQueryParameter("q", query)
            .appendQueryParameter("referrer", referrer)
            .build()
    }
}
