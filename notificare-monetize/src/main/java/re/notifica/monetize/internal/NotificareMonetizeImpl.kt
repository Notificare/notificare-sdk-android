package re.notifica.monetize.internal

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.*
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.models.NotificareApplication
import re.notifica.monetize.NotificareMonetize
import re.notifica.monetize.models.NotificareProduct

internal object NotificareMonetizeImpl : NotificareModule(), NotificareMonetize {

    private var serviceManager: ServiceManager? = null
    private val _observableProducts = MutableLiveData<List<NotificareProduct>>()

    // region Notificare Module

    override fun configure() {
        serviceManager = ServiceManager.create()

        serviceManager?.observableProducts?.observeForever {
            _observableProducts.postValue(it)
        }
    }

    override suspend fun launch() {
        checkNotNull(serviceManager) { "No monetize dependencies have been detected. Please include one of the platform-specific monetize packages." }
            .startConnection()
    }

    override suspend fun unlaunch() {
        checkNotNull(serviceManager) { "No monetize dependencies have been detected. Please include one of the platform-specific monetize packages." }
            .stopConnection()
    }

    // endregion

    // region Notificare Monetize

    override val products: List<NotificareProduct>
        get() = _observableProducts.value ?: listOf()

    override val observableProducts: LiveData<List<NotificareProduct>> = _observableProducts

    override suspend fun refresh(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        checkNotNull(serviceManager) { "No monetize dependencies have been detected. Please include one of the platform-specific monetize packages." }
            .refresh()
    }

    override fun refresh(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::refresh)(callback)

    override fun startPurchaseFlow(activity: Activity, product: NotificareProduct) {
        checkPrerequisites()

        checkNotNull(serviceManager) { "No monetize dependencies have been detected. Please include one of the platform-specific monetize packages." }
            .startPurchaseFlow(activity, product)
    }

    // endregion

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.IN_APP_PURCHASE] != true) {
            NotificareLogger.warning("Notificare in-app purchase functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.IN_APP_PURCHASE)
        }
    }
}
