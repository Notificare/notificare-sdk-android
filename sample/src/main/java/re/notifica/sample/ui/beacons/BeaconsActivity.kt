package re.notifica.sample.ui.beacons

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import re.notifica.geo.NotificareGeo
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion
import re.notifica.sample.R
import re.notifica.sample.databinding.BeaconRowBinding
import re.notifica.sample.databinding.BeaconsActivityBinding

class BeaconsActivity : AppCompatActivity(), NotificareGeo.Listener {

    private lateinit var binding: BeaconsActivityBinding
    private lateinit var adapter: BeaconListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BeaconsActivityBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.list.adapter = BeaconListAdapter().also { adapter = it }
    }

    override fun onStart() {
        super.onStart()

        NotificareGeo.addListener(this)
    }

    override fun onStop() {
        super.onStop()

        NotificareGeo.removeListener(this)
    }


    // region NotificareGeo.Listener

    override fun onLocationUpdated(location: NotificareLocation) {
        Toast.makeText(this, "location = (${location.latitude}, ${location.longitude})", Toast.LENGTH_SHORT).show()
    }

    override fun onBeaconsRanged(region: NotificareRegion, beacons: List<NotificareBeacon>) {
        adapter.setData(region, beacons)
    }

    // endregion


    private class BeaconListAdapter : RecyclerView.Adapter<BeaconListAdapter.BeaconViewHolder>() {

        private var region: NotificareRegion? = null
        private var beacons = listOf<NotificareBeacon>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
            return BeaconViewHolder(parent)
        }

        override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
            holder.bind(beacons[position])
        }

        override fun getItemCount(): Int {
            return beacons.size
        }


        @SuppressLint("NotifyDataSetChanged")
        fun setData(region: NotificareRegion, beacons: List<NotificareBeacon>) {
            this.region = region
            this.beacons = beacons

            this.notifyDataSetChanged()
        }


        private class BeaconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val binding = BeaconRowBinding.bind(itemView)

            fun bind(beacon: NotificareBeacon) {
                binding.name.text = beacon.name
                binding.details.text = "${beacon.major}:${beacon.minor}"

                when (beacon.proximity) {
                    NotificareBeacon.Proximity.IMMEDIATE -> binding.proximity.setImageResource(R.drawable.ic_signal_wifi_4_bar)
                    NotificareBeacon.Proximity.NEAR -> binding.proximity.setImageResource(R.drawable.ic_signal_wifi_3_bar)
                    NotificareBeacon.Proximity.FAR -> binding.proximity.setImageResource(R.drawable.ic_signal_wifi_1_bar)
                    null -> binding.proximity.setImageDrawable(null)
                }
            }

            companion object {
                operator fun invoke(parent: ViewGroup): BeaconViewHolder {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.beacon_row, parent, false)
                    return BeaconViewHolder(view)
                }
            }
        }
    }
}
