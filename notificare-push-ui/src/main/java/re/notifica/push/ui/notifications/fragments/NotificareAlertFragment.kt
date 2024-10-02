package re.notifica.push.ui.notifications.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment

public class NotificareAlertFragment : NotificationFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FrameLayout(inflater.context)
    }
}
