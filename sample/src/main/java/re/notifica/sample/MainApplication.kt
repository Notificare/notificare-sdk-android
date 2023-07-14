package re.notifica.sample

import android.app.Application
import re.notifica.Notificare
import timber.log.Timber

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        Notificare.launch()
    }
}
