package re.notifica.sample.user.inbox.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import re.notifica.sample.user.inbox.ktx.Event

internal abstract class BaseFragment : Fragment() {
    internal abstract val baseViewModel: BaseViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        baseViewModel.eventsFlow
            .onEach { event ->
                event as Event.ShowSnackBar
                Snackbar.make(requireView(), event.text, Snackbar.LENGTH_SHORT)
                    .show()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }
}
