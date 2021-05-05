package re.notifica.push.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import re.notifica.NotificareLogger

open class NotificarePushUIIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            INTENT_ACTION_CUSTOM_ACTION -> {
                onCustomActionReceived(checkNotNull(intent.data))
            }
        }
    }

    protected open fun onCustomActionReceived(uri: Uri) {
        NotificareLogger.info("Action received, please override onActionReceived if you want to receive these intents.")
    }

    companion object {
        const val INTENT_ACTION_CUSTOM_ACTION = "re.notifica.intent.action.CustomAction"
    }
}
