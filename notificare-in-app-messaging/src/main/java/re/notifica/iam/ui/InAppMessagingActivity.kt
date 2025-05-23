package re.notifica.iam.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import re.notifica.Notificare
import re.notifica.iam.R
import re.notifica.iam.databinding.NotificareInAppMessagingActivityBinding
import re.notifica.iam.internal.logger
import re.notifica.iam.ktx.INTENT_EXTRA_IN_APP_MESSAGE
import re.notifica.iam.ktx.inAppMessagingImplementation
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.utilities.threading.onMainThread
import re.notifica.utilities.parcel.parcelable

public open class InAppMessagingActivity : AppCompatActivity() {

    private lateinit var binding: NotificareInAppMessagingActivityBinding
    private var backgroundTimestamp: Long? = null

    protected lateinit var message: NotificareInAppMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        }

        binding = NotificareInAppMessagingActivityBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        message = savedInstanceState?.parcelable(Notificare.INTENT_EXTRA_IN_APP_MESSAGE)
            ?: intent.parcelable(Notificare.INTENT_EXTRA_IN_APP_MESSAGE)
            ?: throw IllegalStateException("Cannot create the UI without the associated in-app message.")

        if (savedInstanceState != null) {
            val backgroundTimestamp = if (savedInstanceState.containsKey(INTENT_EXTRA_BACKGROUND_TIMESTAMP)) {
                savedInstanceState.getLong(INTENT_EXTRA_BACKGROUND_TIMESTAMP)
            } else {
                null
            }

            val expired = backgroundTimestamp != null &&
                Notificare.inAppMessagingImplementation().hasExpiredBackgroundPeriod(backgroundTimestamp)

            if (expired) {
                logger.debug(
                    "Dismissing the current in-app message for being in the background for longer than the grace period."
                )
                return finish()
            }
        }

        if (savedInstanceState == null) {
            val klass = getFragmentClass(message) ?: run {
                logger.warning("Unsupported in-app message type '${message.type}'.")
                return finish()
            }

            val arguments = bundleOf(Notificare.INTENT_EXTRA_IN_APP_MESSAGE to message)

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, klass, arguments)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val backgroundTimestamp = this.backgroundTimestamp
        val expired = backgroundTimestamp != null &&
            Notificare.inAppMessagingImplementation().hasExpiredBackgroundPeriod(backgroundTimestamp)

        if (expired) {
            logger.debug(
                "Dismissing the current in-app message for being in the background for longer than the grace period."
            )
            return finish()
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Notificare.INTENT_EXTRA_IN_APP_MESSAGE, message)
        backgroundTimestamp?.also { outState.putLong(INTENT_EXTRA_BACKGROUND_TIMESTAMP, it) }
    }

    override fun finish() {
        super.finish()

        // Disable the animation transition.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        onMainThread {
            Notificare.inAppMessagingImplementation().lifecycleListeners.forEach {
                it.get()?.onMessageFinishedPresenting(message)
            }
        }
    }

    public companion object {
        private const val INTENT_EXTRA_BACKGROUND_TIMESTAMP = "re.notifica.intent.extra.BackgroundTimestamp"

        internal fun show(activity: Activity, message: NotificareInAppMessage) {
            activity.startActivity(
                Intent(activity, InAppMessagingActivity::class.java)
                    .putExtra(Notificare.INTENT_EXTRA_IN_APP_MESSAGE, message)
            )

            // Disable the animation transition.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        }

        private fun getFragmentClass(message: NotificareInAppMessage): Class<out Fragment>? {
            return when (message.type) {
                NotificareInAppMessage.TYPE_BANNER -> InAppMessagingBannerFragment::class.java
                NotificareInAppMessage.TYPE_CARD -> InAppMessagingCardFragment::class.java
                NotificareInAppMessage.TYPE_FULLSCREEN -> InAppMessagingFullscreenFragment::class.java
                else -> null
            }
        }
    }
}
