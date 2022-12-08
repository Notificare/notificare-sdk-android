package re.notifica.sample

import android.app.Application
import android.os.StrictMode
import re.notifica.Notificare

class MainApplication : Application() {

    override fun onCreate() {
        enableStrictMode()
        super.onCreate()

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
