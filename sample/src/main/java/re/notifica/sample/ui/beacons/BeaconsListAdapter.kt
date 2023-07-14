package re.notifica.sample.ui.beacons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import re.notifica.geo.models.NotificareBeacon
import re.notifica.sample.R
import re.notifica.sample.databinding.BeaconRowBinding

class BeaconsListAdapter : ListAdapter<NotificareBeacon, RecyclerView.ViewHolder>(BeaconsDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            BeaconRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as ItemViewHolder).bind(item)
    }

    private inner class ItemViewHolder(
        private val binding: BeaconRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificareBeacon) {
            binding.name.text = item.name
            binding.details.text = "${item.major}:${item.minor}"

            when (item.proximity) {
                NotificareBeacon.Proximity.IMMEDIATE -> binding.proximity.setImageResource(R.drawable.ic_signal_wifi_4_bar)
                NotificareBeacon.Proximity.NEAR -> binding.proximity.setImageResource(R.drawable.ic_signal_wifi_3_bar)
                NotificareBeacon.Proximity.FAR -> binding.proximity.setImageResource(R.drawable.ic_signal_wifi_1_bar)
                NotificareBeacon.Proximity.UNKNOWN -> binding.proximity.setImageDrawable(null)
            }
        }
    }
}

private class BeaconsDiffCallback : DiffUtil.ItemCallback<NotificareBeacon>() {
    override fun areItemsTheSame(oldItem: NotificareBeacon, newItem: NotificareBeacon): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NotificareBeacon, newItem: NotificareBeacon): Boolean {
        return oldItem == newItem
    }
}
