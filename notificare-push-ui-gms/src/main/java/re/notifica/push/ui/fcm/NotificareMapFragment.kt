package re.notifica.push.ui.fcm

import android.os.Bundle
import androidx.annotation.Keep
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import re.notifica.Notificare
import re.notifica.internal.common.waitForLayout
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.fcm.ktx.pushUIInternal

@Keep
public class NotificareMapFragment : SupportMapFragment(), OnMapReadyCallback {

    private lateinit var notification: NotificareNotification
    //private lateinit var callback: NotificationFragment.Callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO callback
//        try {
//            callback = parentFragment as Callback
//        } catch (e: ClassCastException) {
//            throw ClassCastException("Parent fragment must implement NotificationFragment.Callback.")
//        }

        notification = savedInstanceState?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: arguments?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
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

        val zoomBounds = LatLngBounds.Builder()
        val markers = mutableListOf<Marker>()

        notification.content
            .filter { it.type == "re.notifica.content.Marker" }
            .forEach {
                val data = it.data as? Map<*, *>
                val latitude = data?.get("latitude") as? Double
                val longitude = data?.get("longitude") as? Double

                if (latitude != null && longitude != null) {
                    val coordinates = LatLng(latitude, longitude)
                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(coordinates)
                            .title(data["title"] as? String)
                            .snippet(data["description"] as? String)
                    )

                    if (marker != null) {
                        markers.add(marker)
                    }

                    zoomBounds.include(coordinates)
                }
            }

        if (markers.isNotEmpty()) {
            view?.waitForLayout {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(zoomBounds.build(), 50))
            }
        }

        Notificare.pushUIInternal().lifecycleListeners.forEach { it.onNotificationPresented(notification) }
    }

    // endregion
}
