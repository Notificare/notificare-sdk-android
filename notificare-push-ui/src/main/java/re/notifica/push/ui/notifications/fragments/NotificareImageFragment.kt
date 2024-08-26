package re.notifica.push.ui.notifications.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.utilities.onMainThread
import re.notifica.utilities.ktx.parcelable
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.databinding.NotificareNotificationImageFragmentBinding
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment
import re.notifica.utilities.loadImage

public class NotificareImageFragment : NotificationFragment() {

    private lateinit var binding: NotificareNotificationImageFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NotificareNotificationImageFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isAdded) {
            binding.pager.adapter = ImageAdapter(notification, this)
        }

        if (notification.content.isEmpty()) {
            onMainThread {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.get()?.onNotificationFailedToPresent(notification)
                }
            }
        } else {
            onMainThread {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.get()?.onNotificationPresented(notification)
                }
            }
        }
    }

    public class ImageAdapter(
        private val notification: NotificareNotification,
        fragment: Fragment
    ) : FragmentStateAdapter(fragment) {

        override fun createFragment(position: Int): Fragment {
            return ImageChildFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(SAVED_STATE_CONTENT, notification.content[position])
                }
            }
        }

        override fun getItemCount(): Int {
            return notification.content.size
        }
    }

    public class ImageChildFragment : Fragment() {
        private lateinit var content: NotificareNotification.Content

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            content = savedInstanceState?.parcelable(SAVED_STATE_CONTENT)
                ?: arguments?.parcelable(SAVED_STATE_CONTENT)
                ?: throw IllegalArgumentException("Missing required notification content parameter.")

            NotificareLogger.info(content.data.toString())
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return ImageView(inflater.context)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            loadImage(requireContext(), content.data as String, view as ImageView)
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putParcelable(SAVED_STATE_CONTENT, content)
        }
    }

    public companion object {
        private const val SAVED_STATE_CONTENT = "re.notifica.ui.Content"
    }
}
