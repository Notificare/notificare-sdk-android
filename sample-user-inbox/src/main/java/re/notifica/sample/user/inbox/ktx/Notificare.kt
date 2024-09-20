package re.notifica.sample.user.inbox.ktx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import re.notifica.Notificare.requireContext
import re.notifica.push.NotificarePush

val NotificarePush.hasNotificationsPermission: Boolean
    get() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(requireContext().applicationContext).areNotificationsEnabled()
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        return granted
    }
