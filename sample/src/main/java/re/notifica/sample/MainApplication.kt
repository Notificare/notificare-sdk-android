package re.notifica.sample

import android.app.Application
import android.os.Build
import android.os.StrictMode
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import re.notifica.push.ktx.push
import re.notifica.sample.live_activities.LiveActivitiesController
import timber.log.Timber

class MainApplication : Application() {
    private val mainScope = MainScope()

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

        mainScope.launch {
            try {
                if (Notificare.canEvaluateDeferredLink()) {
                    val evaluated = Notificare.evaluateDeferredLink()
                    Timber.i("deferred link evaluation = $evaluated")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to evaluate the deferred link.")
            }
        }
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
