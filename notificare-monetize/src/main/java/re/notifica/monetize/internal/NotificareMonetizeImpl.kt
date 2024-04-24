package re.notifica.monetize.internal

import android.app.Activity
import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareApplicationUnavailableException
import re.notifica.NotificareCallback
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.NotificareNotReadyException
import re.notifica.NotificareServiceUnavailableException
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.common.onMainThread
import re.notifica.internal.ktx.coroutineScope
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.moshi
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.models.NotificareApplication
import re.notifica.monetize.NotificareInternalMonetize
import re.notifica.monetize.NotificareMonetize
import re.notifica.monetize.internal.database.MonetizeDatabase
import re.notifica.monetize.internal.database.entities.PurchaseEntity
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchase
import re.notifica.monetize.models.NotificarePurchaseVerification
import java.lang.ref.WeakReference

@Keep
internal object NotificareMonetizeImpl : NotificareModule(), NotificareMonetize, NotificareInternalMonetize {

    private lateinit var database: MonetizeDatabase
    private var serviceManager: ServiceManager? = null
    private val _observableProducts = MutableLiveData<List<NotificareProduct>>(listOf())
    private val _observablePurchases = MutableLiveData<List<NotificarePurchase>>(listOf())

    private val listeners = mutableListOf<WeakReference<NotificareMonetize.Listener>>()

    // region Notificare Module

    override fun configure() {
        val context = Notificare.requireContext()

        database = MonetizeDatabase.create(context)

        serviceManager = ServiceManager.create(
            onBillingSetupFinished = {
                onMainThread {
                    listeners.forEach { it.get()?.onBillingSetupFinished() }

                    Notificare.coroutineScope.launch {
                        try {
                            val adapter = Notificare.moshi.adapter(NotificarePurchase::class.java)
                            val purchases = checkNotNull(serviceManager).fetchPurchases()

                            for (purchase in purchases) {
                                try {
                                    val exists = database.purchases().getPurchaseByOrderId(purchase.id) != null

                                    val entity = PurchaseEntity(
                                        id = purchase.id,
                                        productIdentifier = purchase.productIdentifier,
                                        time = purchase.time,
                                        originalJson = purchase.originalJson,
                                        purchaseJson = adapter.toJson(purchase),
                                    )

                                    database.purchases().insert(entity)

                                    if (exists) {
                                        NotificareLogger.debug("Purchase '${purchase.id}' already exists. Skipping...")
                                    } else {
                                        NotificareLogger.debug("Restoring purchase '${purchase.id}'.")
                                        onMainThread {
                                            listeners.forEach { it.get()?.onPurchaseRestored(purchase) }
                                        }
                                    }
                                } catch (e: Exception) {
                                    NotificareLogger.error("Failed to restore purchase '${purchase.id}'.", e)
                                }
                            }
                        } catch (e: Exception) {
                            NotificareLogger.error("Failed to restore previous purchases.", e)
                        }
                    }
                }
            },
            onBillingSetupFailed = { code, message ->
                onMainThread {
                    listeners.forEach { it.get()?.onBillingSetupFailed(code, message) }
                }
            },
            onProductsUpdated = {
                _observableProducts.postValue(it)
            },
            onPurchaseFinished = { purchase ->
                onMainThread {
                    listeners.forEach { it.get()?.onPurchaseFinished(purchase) }

                    Notificare.coroutineScope.launch {
                        try {
                            val adapter = Notificare.moshi.adapter(NotificarePurchase::class.java)

                            val entity = PurchaseEntity(
                                id = purchase.id,
                                productIdentifier = purchase.productIdentifier,
                                time = purchase.time,
                                originalJson = purchase.originalJson,
                                purchaseJson = adapter.toJson(purchase),
                            )

                            database.purchases().insert(entity)
                        } catch (e: Exception) {
                            NotificareLogger.error("Failed to store purchase '${purchase.id}' on the database.", e)
                        }
                    }
                }
            },
            onPurchaseCanceled = {
                onMainThread {
                    listeners.forEach { it.get()?.onPurchaseCanceled() }
                }
            },
            onPurchaseFailed = { code, message ->
                onMainThread {
                    listeners.forEach { it.get()?.onPurchaseFailed(code, message) }
                }
            },
        )

        database.purchases().getObservablePurchases().observeForever { purchases ->
            if (purchases == null) {
                _observablePurchases.postValue(listOf())
                return@observeForever
            }

            val adapter = Notificare.moshi.adapter(NotificarePurchase::class.java)
            _observablePurchases.postValue(
                purchases.mapNotNull { entity ->
                    try {
                        adapter.fromJson(entity.purchaseJson)
                    } catch (e: Exception) {
                        NotificareLogger.warning("Failed to decode stored purchase '${entity.id}'.", e)
                        null
                    }
                }
            )
        }
    }

    override suspend fun launch() {
        checkNotNull(serviceManager) {
            "No monetize dependencies have been detected. Please include one of the platform-specific monetize packages."
        }.startConnection()
    }

    override suspend fun unlaunch() {
        checkNotNull(serviceManager) {
            "No monetize dependencies have been detected. Please include one of the platform-specific monetize packages."
        }.stopConnection()

        database.purchases().clear()

        _observableProducts.postValue(listOf())
        _observablePurchases.postValue(listOf())
    }

    // endregion

    // region Notificare Monetize

    override val products: List<NotificareProduct>
        get() = _observableProducts.value ?: listOf()

    override val observableProducts: LiveData<List<NotificareProduct>> = _observableProducts

    override val purchases: List<NotificarePurchase>
        get() = _observablePurchases.value ?: listOf()

    override val observablePurchases: LiveData<List<NotificarePurchase>> = _observablePurchases

    override fun addListener(listener: NotificareMonetize.Listener) {
        listeners.add(WeakReference(listener))
    }

    override fun removeListener(listener: NotificareMonetize.Listener) {
        listeners.forEach { reference ->
            val referent = reference.get()
            if (referent == null || referent == listener) {
                listeners.remove(reference)
            }
        }
    }

    override suspend fun refresh(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        checkNotNull(serviceManager) {
            "No monetize dependencies have been detected. Please include one of the platform-specific monetize packages."
        }.refresh()
    }

    override fun refresh(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::refresh)(callback)

    override fun startPurchaseFlow(activity: Activity, product: NotificareProduct) {
        checkPrerequisites()

        checkNotNull(serviceManager) {
            "No monetize dependencies have been detected. Please include one of the platform-specific monetize packages."
        }.startPurchaseFlow(activity, product)
    }

    // endregion

    // region Notificare Internal Monetize

    override suspend fun verifyPurchase(purchase: NotificarePurchaseVerification): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice ?: throw NotificareDeviceUnavailableException()

        NotificareRequest.Builder()
            .post("/purchase/fordevice/${device.id}", purchase)
            .response()
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
