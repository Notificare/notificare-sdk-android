package re.notifica.push.ui.notifications.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import re.notifica.Notificare
import re.notifica.utilities.threading.onMainThread
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment

@Keep
public class NotificareRateFragment : NotificationFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState).also {
            try {
                val uri = Uri.parse("market://details?id=${inflater.context.packageName}")
                val rateIntent = Intent(Intent.ACTION_VIEW, uri)

                callback.onNotificationFragmentStartActivity(rateIntent)
                callback.onNotificationFragmentFinished()

                onMainThread {
                    Notificare.pushUIInternal().lifecycleListeners.forEach {
                        it.get()?.onNotificationPresented(
                            notification
                        )
                    }
                }
            } catch (_: ActivityNotFoundException) {
                val uri = Uri.parse("https://play.google.com/store/apps/details?id=${inflater.context.packageName}")
                val rateIntent = Intent(Intent.ACTION_VIEW, uri)

                callback.onNotificationFragmentStartActivity(rateIntent)
                callback.onNotificationFragmentFinished()

                onMainThread {
                    Notificare.pushUIInternal().lifecycleListeners.forEach {
                        it.get()?.onNotificationFailedToPresent(
                            notification
                        )
                    }
                }
            }
        }
    }
}
