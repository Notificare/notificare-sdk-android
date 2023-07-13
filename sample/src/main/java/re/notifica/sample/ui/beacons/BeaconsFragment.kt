package re.notifica.sample.ui.beacons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import re.notifica.Notificare
import re.notifica.geo.NotificareGeo
import re.notifica.geo.ktx.geo
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion
import re.notifica.sample.databinding.FragmentBeaconsBinding

class BeaconsFragment : Fragment(), NotificareGeo.Listener {
    private lateinit var binding: FragmentBeaconsBinding
    private val adapter = BeaconsListAdapter()
    private val viewModel: BeaconsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBeaconsBinding.inflate(inflater, container, false)

        binding.beaconsList.layoutManager = LinearLayoutManager(requireContext())
        binding.beaconsList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.beaconsList.adapter = adapter

        setupObservers()

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        Notificare.geo().addListener(this)
    }

    override fun onStop() {
        super.onStop()

        Notificare.geo().removeListener(this)
    }

    override fun onLocationUpdated(location: NotificareLocation) {
        Snackbar.make(requireView(), "location = (${location.latitude}, ${location.longitude})", Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun onBeaconsRanged(region: NotificareRegion, beacons: List<NotificareBeacon>) {
        val beaconsRanged = BeaconsViewModel.BeaconsData(region, beacons)

        binding.noBeaconsLabel.isVisible = beacons.isEmpty()
        viewModel.beaconsData.postValue(beaconsRanged)
    }

    private fun setupObservers() {
        viewModel.beaconsData.observe(viewLifecycleOwner) { beaconsData ->
            adapter.submitList(beaconsData.beacons)
        }
    }
}
