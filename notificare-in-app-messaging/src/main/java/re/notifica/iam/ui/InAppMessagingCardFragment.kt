package re.notifica.iam.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import re.notifica.iam.R
import re.notifica.iam.databinding.NotificareInAppMessageCardFragmentBinding
import re.notifica.iam.internal.caching.NotificareImageCache
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.iam.ui.base.InAppMessagingBaseFragment

public open class InAppMessagingCardFragment : InAppMessagingBaseFragment() {
    private lateinit var binding: NotificareInAppMessageCardFragmentBinding

    override val animatedView: View
        get() = binding.cardView

    override val enterAnimation: Int = R.anim.notificare_iam_card_enter_animation
    override val exitAnimation: Int = R.anim.notificare_iam_card_exit_animation

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = NotificareInAppMessageCardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val image = NotificareImageCache.getOrientationConstrainedImage(requireContext())

        binding.imageView.isVisible = image != null
        binding.imageView.setImageBitmap(image)

        binding.titleView.isVisible = !message.title.isNullOrBlank()
        binding.titleView.text = message.title

        binding.messageView.isVisible = !message.message.isNullOrBlank()
        binding.messageView.text = message.message

        binding.actionsContainer.isVisible =
            !message.primaryAction?.label.isNullOrBlank() || !message.secondaryAction?.label.isNullOrBlank()

        binding.primaryActionButton.isVisible = canShowAction(message.primaryAction)
        binding.primaryActionButton.text = message.primaryAction?.label
        binding.primaryActionButton.setOnClickListener {
            handleActionClicked(NotificareInAppMessage.ActionType.PRIMARY)
        }

        if (message.primaryAction?.destructive == true) {
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(R.attr.colorError, typedValue, true)
            binding.primaryActionButton.setTextColor(typedValue.data)
        }

        binding.secondaryActionButton.isVisible = canShowAction(message.secondaryAction)
        binding.secondaryActionButton.text = message.secondaryAction?.label
        binding.secondaryActionButton.setOnClickListener {
            handleActionClicked(NotificareInAppMessage.ActionType.SECONDARY)
        }

        if (message.secondaryAction?.destructive == true) {
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(R.attr.colorError, typedValue, true)
            binding.secondaryActionButton.setTextColor(typedValue.data)
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.cardView.setOnClickListener {
            // This empty click listener prevents the root click listener
            // from being triggered when clicking on the card itself.
            //
            // Otherwise, the root click listener will treat clicking on
            // the card itself as a tap outside.
        }

        binding.scrollableContent.setOnClickListener {
            dismiss()
        }

        if (savedInstanceState == null) {
            animate(Transition.ENTER)
        }
    }

    private fun canShowAction(action: NotificareInAppMessage.Action?): Boolean {
        if (action == null) return false
        if (action.label.isNullOrBlank()) return false
        if (action.url.isNullOrBlank()) return false

        return true
    }
}
