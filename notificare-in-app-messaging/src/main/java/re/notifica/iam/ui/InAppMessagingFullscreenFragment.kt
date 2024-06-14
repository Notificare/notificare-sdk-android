package re.notifica.iam.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import re.notifica.iam.R
import re.notifica.iam.databinding.NotificareInAppMessageFullscreenFragmentBinding
import re.notifica.iam.internal.caching.NotificareImageCache
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.iam.ui.base.InAppMessagingBaseFragment

public open class InAppMessagingFullscreenFragment : InAppMessagingBaseFragment() {
    private lateinit var binding: NotificareInAppMessageFullscreenFragmentBinding

    override val animatedView: View
        get() = binding.cardView

    override val enterAnimation: Int = R.anim.notificare_iam_fullscreen_enter_animation
    override val exitAnimation: Int = R.anim.notificare_iam_fullscreen_exit_animation

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = NotificareInAppMessageFullscreenFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val image = NotificareImageCache.getOrientationConstrainedImage(requireContext())

        binding.imageView.isVisible = image != null
        binding.imageView.setImageBitmap(image)

        binding.contentContainer.isVisible = !message.title.isNullOrBlank() || !message.message.isNullOrBlank()

        binding.titleView.isVisible = !message.title.isNullOrBlank()
        binding.titleView.text = message.title

        binding.messageView.isVisible = !message.message.isNullOrBlank()
        binding.messageView.text = message.message

        binding.cardView.setOnClickListener {
            handleActionClicked(NotificareInAppMessage.ActionType.PRIMARY)
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.root.setOnClickListener {
            dismiss()
        }

        if (savedInstanceState == null) {
            animate(Transition.ENTER)
        }
    }
}
