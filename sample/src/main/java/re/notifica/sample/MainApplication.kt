package re.notifica.sample

import android.app.Application
import android.os.StrictMode
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import timber.log.Timber

class MainApplication : Application() {

    override fun onCreate() {
        enableStrictMode()
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        Notificare.geo().intentReceiver = SampleGeoIntentReceiver::class.java

        Notificare.launch()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
    }
}
