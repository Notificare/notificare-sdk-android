package re.notifica.push.ui.notifications.fragments

import android.os.Bundle
import androidx.annotation.Keep
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import re.notifica.Notificare
import re.notifica.internal.common.onMainThread
import re.notifica.utilities.waitForLayout
import re.notifica.internal.ktx.parcelable
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.ktx.pushUIInternal

@Keep
public class NotificareMapFragment : SupportMapFragment(), OnMapReadyCallback {

    private lateinit var notification: NotificareNotification
    // private lateinit var callback: NotificationFragment.Callback

    private val notificationMarkers: List<NotificationMarker> by lazy {
        notification.content
            .filter { it.type == "re.notifica.content.Marker" }
            .mapNotNull { content ->
                val data = content.data as? Map<*, *> ?: return@mapNotNull null
                val latitude = data["latitude"] as? Double ?: return@mapNotNull null
                val longitude = data["longitude"] as? Double ?: return@mapNotNull null

                return@mapNotNull NotificationMarker(
                    latitude = latitude,
                    longitude = longitude,
                    title = data["title"] as? String,
                    description = data["description"] as? String,
                )
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO callback
//        try {
//            callback = parentFragment as Callback
//        } catch (e: ClassCastException) {
//            throw ClassCastException("Parent fragment must implement NotificationFragment.Callback.")
//        }

        notification = savedInstanceState?.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: arguments?.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: throw IllegalArgumentException("Missing required notification parameter.")

        getMapAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
    }

    // region OnMapReadyCallback

    override fun onMapReady(map: GoogleMap) {
        map.uiSettings.isRotateGesturesEnabled = false
        map.uiSettings.isZoomControlsEnabled = false

        populateMarkers(map)
        configureMapZoom(map)

        onMainThread {
            Notificare.pushUIInternal().lifecycleListeners.forEach {
                it.get()?.onNotificationPresented(
                    notification
                )
            }
        }
    }

    // endregion

    private fun populateMarkers(map: GoogleMap) {
        notificationMarkers.forEach { marker ->
            val coordinates = LatLng(marker.latitude, marker.longitude)

            map.addMarker(
                MarkerOptions()
                    .position(coordinates)
                    .title(marker.title)
                    .snippet(marker.description)
            )
        }
    }

    private fun configureMapZoom(map: GoogleMap) {
        if (notificationMarkers.isEmpty()) return

        if (notificationMarkers.size == 1) {
            val marker = notificationMarkers.first()

            view?.waitForLayout {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(marker.latitude, marker.longitude), 17f))
            }

            return
        }

        val zoomBounds = LatLngBounds.Builder().apply {
            notificationMarkers.forEach {
                include(LatLng(it.latitude, it.longitude))
            }
        }.build()

        view?.waitForLayout {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(zoomBounds, 50))
        }
    }

    private data class NotificationMarker(
        val latitude: Double,
        val longitude: Double,
        val title: String?,
        val description: String?,
    )
}
