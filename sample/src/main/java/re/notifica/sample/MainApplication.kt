package re.notifica.sample

import android.app.Application
import re.notifica.Notificare

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Notificare.launch()
    }
}
