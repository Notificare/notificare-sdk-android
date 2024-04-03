package re.notifica.push.ui.gms

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import re.notifica.Notificare
import re.notifica.internal.common.onMainThread
import re.notifica.push.ui.gms.ktx.pushUIInternal
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
                        it.onNotificationPresented(
                            notification
                        )
                    }
                }
            } catch (e: ActivityNotFoundException) {
                val uri = Uri.parse(
                    "https://play.google.com/store/apps/details?id=${inflater.context.packageName}"
                )
                val rateIntent = Intent(Intent.ACTION_VIEW, uri)

                callback.onNotificationFragmentStartActivity(rateIntent)
                callback.onNotificationFragmentFinished()

                onMainThread {
                    Notificare.pushUIInternal().lifecycleListeners.forEach {
                        it.onNotificationFailedToPresent(
                            notification
                        )
                    }
                }
            }
        }
    }
}
