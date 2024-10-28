package re.notifica.sample.user.inbox

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.push.ktx.push
import timber.log.Timber

class MainApplication : Application() {
    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        Notificare.push().intentReceiver = SamplePushIntentReceiver::class.java

        applicationScope.launch {
            try {
                Notificare.launch()
            } catch (e: Exception) {
                Timber.e(e, "Failed to launch Notificare.")
            }
        }
    }
}
