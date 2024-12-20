package re.notifica.iam.ui.base

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.iam.internal.caching.NotificareImageCache
import re.notifica.iam.internal.logger
import re.notifica.iam.ktx.INTENT_EXTRA_IN_APP_MESSAGE
import re.notifica.iam.ktx.inAppMessagingImplementation
import re.notifica.iam.ktx.logInAppMessageActionClicked
import re.notifica.iam.ktx.logInAppMessageViewed
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.utilities.threading.onMainThread
import re.notifica.utilities.parcel.parcelable
import re.notifica.ktx.events

public abstract class InAppMessagingBaseFragment : Fragment() {
    protected lateinit var message: NotificareInAppMessage

    protected abstract val animatedView: View
    protected abstract val enterAnimation: Int
    protected abstract val exitAnimation: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        message = savedInstanceState?.parcelable(Notificare.INTENT_EXTRA_IN_APP_MESSAGE)
            ?: arguments?.parcelable(Notificare.INTENT_EXTRA_IN_APP_MESSAGE)
            ?: throw IllegalStateException("Cannot create the UI without the associated in-app message.")
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Prevent tracking the event during configuration changes.
        if (savedInstanceState != null) return

        logger.debug("Tracking in-app message viewed event.")
        lifecycleScope.launch {
            try {
                Notificare.events().logInAppMessageViewed(message)
            } catch (e: Exception) {
                logger.error("Failed to log in-app message viewed event.", e)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Notificare.INTENT_EXTRA_IN_APP_MESSAGE, message)
    }

    protected fun animate(transition: Transition, onAnimationFinished: () -> Unit = {}) {
        val animation = when (transition) {
            Transition.ENTER -> AnimationUtils.loadAnimation(requireContext(), enterAnimation)
            Transition.EXIT -> AnimationUtils.loadAnimation(requireContext(), exitAnimation)
        }

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                // no-op
            }

            override fun onAnimationEnd(animation: Animation?) {
                onAnimationFinished()
            }

            override fun onAnimationRepeat(animation: Animation?) {
                // no-op
            }
        })

        animatedView.clearAnimation()
        animatedView.animation = animation
    }

    protected fun dismiss() {
        animate(
            transition = Transition.EXIT,
            onAnimationFinished = {
                activity?.finish()
                NotificareImageCache.clear()
            }
        )
    }

    protected fun handleActionClicked(actionType: NotificareInAppMessage.ActionType) {
        val action = when (actionType) {
            NotificareInAppMessage.ActionType.PRIMARY -> message.primaryAction
            NotificareInAppMessage.ActionType.SECONDARY -> message.secondaryAction
        }

        if (action == null) {
            logger.debug("There is no '${actionType.rawValue}' action to process.")
            dismiss()

            return
        }

        if (action.url.isNullOrBlank()) {
            logger.debug("There is no URL for '${actionType.rawValue}' action.")
            dismiss()

            return
        }

        lifecycleScope.launch {
            try {
                Notificare.events().logInAppMessageActionClicked(message, actionType)
            } catch (e: Exception) {
                logger.error("Failed to log in-app message action.", e)
            }

            val uri = action.url.let { Uri.parse(it) }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(requireContext().packageName)
            }

            if (intent.resolveActivity(requireContext().packageManager) == null) {
                intent.setPackage(null)
            }

            try {
                startActivity(intent)
                logger.info("In-app message action '${actionType.rawValue}' successfully processed.")

                onMainThread {
                    Notificare.inAppMessagingImplementation().lifecycleListeners.forEach {
                        it.get()?.onActionExecuted(message, action)
                    }
                }

                activity?.finish()
                NotificareImageCache.clear()
            } catch (e: Exception) {
                logger.warning("Could not find an activity capable of opening the URL.", e)

                onMainThread {
                    Notificare.inAppMessagingImplementation().lifecycleListeners.forEach {
                        it.get()?.onActionFailedToExecute(message, action, e)
                    }
                }

                dismiss()
            }
        }
    }

    protected enum class Transition {
        ENTER,
        EXIT;
    }
}
