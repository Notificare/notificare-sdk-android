package re.notifica.monetize.gms.internal

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.monetize.gms.internal.ktx.isGooglePlay
import re.notifica.monetize.gms.internal.ktx.priceAmount
import re.notifica.monetize.gms.internal.ktx.toModel
import re.notifica.monetize.gms.ktx.monetizeInternal
import re.notifica.monetize.internal.ServiceManager
import re.notifica.monetize.internal.network.push.FetchProductsResponse
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchaseVerification

@InternalNotificareApi
public class ServiceManager : ServiceManager(), BillingClientStateListener, PurchasesUpdatedListener {

    private val billingClient: BillingClient by lazy {
        val context = Notificare.requireContext()

        BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener(this)
            .build()
    }

    private val _observableProducts = MutableLiveData<List<NotificareProduct>>()
    private var products = mapOf<String, NotificareProduct>()       // where K is the Google product identifier
    private var productDetails = mapOf<String, ProductDetails>()    // where K is the Google product identifier


    override val available: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

    override val observableProducts: LiveData<List<NotificareProduct>> = _observableProducts

    override fun startConnection() {
        billingClient.startConnection(this)
    }

    override fun stopConnection() {
        billingClient.endConnection()
    }

    override suspend fun refresh(): Unit = withContext(Dispatchers.IO) {
        val products = fetchProducts()
        val productDetails = fetchProductDetails(products.map { it.identifier })

        val models = products.map { product ->
            val details = productDetails.firstOrNull { it.productId == product.identifier }
            return@map product.toModel(details)
        }

        this@ServiceManager.products = models.associateBy { it.identifier }
        this@ServiceManager.productDetails = productDetails.associateBy { it.productId }

        this@ServiceManager._observableProducts.postValue(models)
    }

    override fun startPurchaseFlow(activity: Activity, product: NotificareProduct) {
        val details = productDetails[product.identifier] ?: run {
            NotificareLogger.warning("Unable to start a purchase flow when the product is not cached.")
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        val result = billingClient.launchBillingFlow(activity, params)

        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            NotificareLogger.info("Purchase flow launched.")
            NotificareLogger.debug("$result")
        } else {
            NotificareLogger.warning("Something went wrong while launching the purchase flow.")
            NotificareLogger.debug("$result")
        }
    }

    // region BillingClientStateListener

    override fun onBillingServiceDisconnected() {
        NotificareLogger.warning("Billing service disconnected. Reconnecting...")
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        NotificareLogger.info("Billing setup finished.")
        NotificareLogger.debug("$result")

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            // TODO: listeners.onBillingSetupFailed(result.responseCode)
            return
        }

        GlobalScope.launch {
            try {
                refresh()
            } catch (e: Exception) {
                NotificareLogger.error("Failed to refresh the products cache.", e)
            }
        }
    }

    // endregion

    // region PurchasesUpdatedListener

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases == null) {
                    NotificareLogger.debug("Purchases updated event with an undefined list.")
                    return
                }

                NotificareLogger.info("Processing purchases event.")
                NotificareLogger.debug("${purchases.count { it.purchaseState == Purchase.PurchaseState.PURCHASED }} purchased items.")
                NotificareLogger.debug("${purchases.count { it.purchaseState == Purchase.PurchaseState.PENDING }} pending items.")
                NotificareLogger.debug("${purchases.count { it.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE }} undefined state items.")

                GlobalScope.launch {
                    try {
                        for (purchase in purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                            try {
                                NotificareLogger.debug("Verifying purchase order '${purchase.orderId}'.")
                                processPurchase(purchase)
                            } catch (e: Exception) {
                                NotificareLogger.error("Failed to process purchase order '${purchase.orderId}'.")
                            }
                        }
                    } catch (e: Exception) {
                        NotificareLogger.error("Something went wrong while processing the purchases.", e)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // TODO listeners.onPurchaseCancelled()
            }
            else -> {
                // TODO listeners.onPurchaseFailure()
            }
        }
    }

    // endregion


    private suspend fun fetchProducts(): List<FetchProductsResponse.Product> = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/product/active")
            .responseDecodable(FetchProductsResponse::class)
            .products
            .filter { it.isGooglePlay }
    }

    private suspend fun fetchProductDetails(
        identifiers: List<String>
    ): List<ProductDetails> = withContext(Dispatchers.IO) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                identifiers.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        val result = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            // TODO: throw exception?
            throw RuntimeException("Failed to fetch the product details from Google Play.")
        }

        val productDetailsList = result.productDetailsList ?: run {
            // TODO: throw exception?
            throw RuntimeException("Fetch product details succeeded with an undefined list.")
        }

        return@withContext productDetailsList
    }

    private suspend fun processPurchase(purchase: Purchase): Unit = withContext(Dispatchers.IO) {
        val identifier = purchase.products.firstOrNull() ?: run {
            NotificareLogger.warning("Unable to process a purchase with no products.")
            return@withContext
        }

        val product = products[identifier]
        val productDetails = productDetails[identifier]
        if (product == null || productDetails == null) {
            NotificareLogger.warning("Unable to process a purchase when the product is not cached.")
            return@withContext
        }

        val oneTimePurchaseOfferDetails = productDetails.oneTimePurchaseOfferDetails ?: run {
            NotificareLogger.warning("Only one time purchases are supported.")
            return@withContext
        }

        Notificare.monetizeInternal().verifyPurchase(
            NotificarePurchaseVerification(
                receipt = purchase.originalJson,
                signature = purchase.signature,
                price = oneTimePurchaseOfferDetails.priceAmount,
                currency = oneTimePurchaseOfferDetails.priceCurrencyCode,
            )
        )

        if (product.type == NotificareProduct.TYPE_CONSUMABLE) {
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val result = billingClient.consumePurchase(params)

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                NotificareLogger.info("Purchase successfully consumed.")
                NotificareLogger.debug("$result")

                // TODO: notify the listeners.
            } else {
                NotificareLogger.warning("Something went wrong while consuming the purchase.")
                NotificareLogger.debug("$result")

                // TODO: notify the listeners.
            }

            return@withContext
        }

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val result = billingClient.acknowledgePurchase(params)

            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                NotificareLogger.info("Purchase successfully acknowledged.")
                NotificareLogger.debug("$result")

                // TODO: notify the listeners.
            } else {
                NotificareLogger.warning("Something went wrong while acknowledging the purchase.")
                NotificareLogger.debug("$result")

                // TODO: notify the listeners.
            }

            return@withContext
        }

        NotificareLogger.debug("No action necessary for purchase order '${purchase.orderId}'.")
    }
}
