package re.notifica.geo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import re.notifica.Notificare
import re.notifica.NotificareException
import re.notifica.geo.internal.ServiceManager
import re.notifica.internal.NotificareLogger
import re.notifica.modules.NotificareModule

public object NotificareGeo : NotificareModule() {

    private var serviceManager: ServiceManager? = null

    private val hasForegroundLocationPermission: Boolean
        get() {
            return if (BuildCompat.isAtLeastS()) {
                ContextCompat.checkSelfPermission(
                    Notificare.requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    Notificare.requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

    private val hasBackgroundLocationPermission: Boolean
        get() {
            val hasBackgroundAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    Notificare.requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            return hasBackgroundAccess && hasForegroundLocationPermission
        }

    public val locationServicesEnabled: Boolean
        get() = false

    // region NotificareModule

    override fun configure() {
        serviceManager = ServiceManager.create()
    }

    override suspend fun launch() {

    }

    override suspend fun unlaunch() {

    }

    // endregion

    public fun enableLocationUpdates() {
        try {
            checkPrerequisites()
        } catch (e: Exception) {
            return
        }

        if (!hasForegroundLocationPermission) {
            // TODO clear location
            return
        }

        serviceManager?.enableLocationUpdates()
    }

    public fun disableLocationUpdates() {
        try {
            checkPrerequisites()
        } catch (e: Exception) {
            return
        }
    }

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareException.NotReady()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareException.NotReady()
        }

        if (application.services["locationServices"] != true) {
            NotificareLogger.warning("Notificare location functionality is not enabled.")
            throw NotificareException.NotReady()
        }

        if (!Notificare.requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)) {
            NotificareLogger.warning("Location functionality requires location hardware.")
            throw NotificareException.NotReady()
        }
    }
}

public fun geo(): NotificareGeo {
    return NotificareGeo
}
