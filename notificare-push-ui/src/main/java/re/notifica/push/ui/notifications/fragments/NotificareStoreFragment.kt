package re.notifica.push.ui.notifications.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.Keep
import re.notifica.Notificare
import re.notifica.internal.common.onMainThread
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.R
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment

@Keep
public class NotificareStoreFragment : NotificationFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FrameLayout(requireContext())
    }

    @Suppress("detekt:NestedBlockDepth")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return

        val referrer = context.packageName
        var uri: Uri? = null
        var altUri: Uri? = null

        var content: NotificareNotification.Content? = notification.content.firstOrNull {
            it.type == NotificareNotification.Content.TYPE_GOOGLE_PLAY_DETAILS
        }

        if (content != null) {
            uri = getDetailsIntentUri(content.data as String, referrer)
            altUri = getDetailsIntentAltUri(content.data as String, referrer)
        } else {
            content = notification.content.firstOrNull {
                it.type == NotificareNotification.Content.TYPE_GOOGLE_PLAY_DEVELOPER
            }

            if (content != null) {
                uri = getDeveloperIntentUri(content.data as String, referrer)
                altUri = getDeveloperIntentAltUri(content.data as String, referrer)
            } else {
                content = notification.content.firstOrNull {
                    it.type == NotificareNotification.Content.TYPE_GOOGLE_PLAY_SEARCH
                }

                if (content != null) {
                    uri = getSearchIntentUri(content.data as String, referrer)
                    altUri = getSearchIntentAltUri(content.data as String, referrer)
                } else {
                    content = notification.content.firstOrNull {
                        it.type == NotificareNotification.Content.TYPE_GOOGLE_PLAY_COLLECTION
                    }

                    if (content != null) {
                        uri = getCollectionIntentUri(content.data as String, referrer)
                        altUri = getCollectionIntentAltUri(content.data as String, referrer)
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
                        it.get()?.onNotificationPresented(notification)
                    }
                }
            } catch (_: ActivityNotFoundException) {
                if (altUri != null) {
                    try {
                        val rateIntent = Intent(Intent.ACTION_VIEW, altUri)

                        callback.onNotificationFragmentStartActivity(rateIntent)
                        callback.onNotificationFragmentFinished()

                        onMainThread {
                            Notificare.pushUIInternal().lifecycleListeners.forEach {
                                it.get()?.onNotificationPresented(notification)
                            }
                        }
                    } catch (_: ActivityNotFoundException) {
                        callback.onNotificationFragmentActionFailed(
                            resources.getString(R.string.notificare_google_play_intent_failed)
                        )
                        callback.onNotificationFragmentFinished()

                        onMainThread {
                            Notificare.pushUIInternal().lifecycleListeners.forEach {
                                it.get()?.onNotificationFailedToPresent(notification)
                            }
                        }
                    }
                } else {
                    callback.onNotificationFragmentActionFailed(
                        resources.getString(R.string.notificare_google_play_intent_failed)
                    )
                    callback.onNotificationFragmentFinished()

                    onMainThread {
                        Notificare.pushUIInternal().lifecycleListeners.forEach {
                            it.get()?.onNotificationFailedToPresent(notification)
                        }
                    }
                }
            }
        } else {
            callback.onNotificationFragmentActionFailed(
                resources.getString(R.string.notificare_google_play_intent_failed)
            )
            callback.onNotificationFragmentFinished()

            onMainThread {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.get()?.onNotificationFailedToPresent(notification)
                }
            }
        }
    }

    private fun getDetailsIntentUri(id: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("market")
            .authority("details")
            .appendQueryParameter("id", id)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun getDeveloperIntentUri(id: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("market")
            .authority("dev")
            .appendQueryParameter("id", id)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun getSearchIntentUri(query: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("market")
            .authority("search")
            .appendQueryParameter("q", query)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun getCollectionIntentUri(name: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("market")
            .authority("apps")
            .appendPath("collection")
            .appendPath(name)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun getDetailsIntentAltUri(id: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("https")
            .authority("play.google.com")
            .appendPath("store")
            .appendPath("apps")
            .appendPath("details")
            .appendQueryParameter("id", id)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun getDeveloperIntentAltUri(id: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("https")
            .authority("play.google.com")
            .appendPath("store")
            .appendPath("apps")
            .appendPath("developer")
            .appendQueryParameter("id", id)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun getSearchIntentAltUri(query: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("https")
            .authority("play.google.com")
            .appendPath("store")
            .appendPath("search")
            .appendQueryParameter("q", query)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun getCollectionIntentAltUri(name: String, referrer: String): Uri {
        return Uri.Builder()
            .scheme("https")
            .authority("play.google.com")
            .appendPath("store")
            .appendPath("apps")
            .appendPath("collection")
            .appendPath(name)
            .appendQueryParameter("referrer", referrer)
            .build()
    }
}
