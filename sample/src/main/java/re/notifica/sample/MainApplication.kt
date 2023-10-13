package re.notifica.sample

import android.app.Application
import android.os.Build
import android.os.StrictMode
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import re.notifica.push.ktx.push
import re.notifica.sample.live_activities.LiveActivitiesController
import timber.log.Timber

class MainApplication : Application() {
    override fun onCreate() {
        enableStrictMode()
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        LiveActivitiesController.setup(this)

        Notificare.geo().intentReceiver = SampleGeoIntentReceiver::class.java

        Notificare.push().intentReceiver = SamplePushIntentReceiver::class.java

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LiveActivitiesController.registerLiveActivitiesChannel()
        }

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
