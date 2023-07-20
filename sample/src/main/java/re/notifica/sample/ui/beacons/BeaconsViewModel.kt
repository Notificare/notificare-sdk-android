package re.notifica.sample.ui.beacons

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareRegion

class BeaconsViewModel : ViewModel() {
    val beaconsData = MutableLiveData<BeaconsData>()

    data class BeaconsData(
        val region: NotificareRegion,
        val beacons: List<NotificareBeacon>
    )
}
