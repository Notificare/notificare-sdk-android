package re.notifica.monetize.gms.internal

import android.app.Activity
import androidx.annotation.Keep
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
import re.notifica.monetize.BillingException
import re.notifica.monetize.gms.internal.ktx.from
import re.notifica.monetize.gms.internal.ktx.isGooglePlay
import re.notifica.monetize.gms.internal.ktx.priceAmount
import re.notifica.monetize.gms.internal.ktx.toModel
import re.notifica.monetize.gms.ktx.monetizeInternal
import re.notifica.monetize.internal.ServiceManager
import re.notifica.monetize.internal.network.push.FetchProductsResponse
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchase
import re.notifica.monetize.models.NotificarePurchaseVerification

@Keep
@InternalNotificareApi
public class ServiceManager : ServiceManager(), BillingClientStateListener, PurchasesUpdatedListener {

    private val billingClient: BillingClient by lazy {
        val context = Notificare.requireContext()

        BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener(this)
            .build()
    }

    private var products = mapOf<String, NotificareProduct>()       // where K is the Google product identifier
    private var productDetails = mapOf<String, ProductDetails>()    // where K is the Google product identifier


    override val available: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(Notificare.requireContext()) == ConnectionResult.SUCCESS

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

        onProductsUpdated(models)
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

    override suspend fun fetchPurchases(): List<NotificarePurchase> = withContext(Dispatchers.IO) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            return@withContext result.purchasesList.map { NotificarePurchase.from(it) }
        } else {
            NotificareLogger.error("Failed to query purchases with error code '${result.billingResult.responseCode}': ${result.billingResult.debugMessage}")
            throw Exception("Failed to query purchases.")
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
            onBillingSetupFailed(result.responseCode, result.debugMessage)
            return
        }

        GlobalScope.launch {
            try {
                refresh()
            } catch (e: Exception) {
                NotificareLogger.error("Failed to refresh the products cache.", e)
            }

            onBillingSetupFinished()
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

                                onPurchaseFinished(NotificarePurchase.from(purchase))
                            } catch (e: Exception) {
                                NotificareLogger.error("Failed to process purchase order '${purchase.orderId}'.", e)
                            }
                        }
                    } catch (e: Exception) {
                        NotificareLogger.error("Something went wrong while processing the purchases.", e)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> onPurchaseCanceled()
            else -> onPurchaseFailed(result.responseCode, result.debugMessage)
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
            throw BillingException(result.billingResult.responseCode, result.billingResult.debugMessage)
        }

        val productDetailsList = result.productDetailsList ?: run {
            throw RuntimeException("Fetch product details succeeded with an undefined list.")
        }

        return@withContext productDetailsList
    }

    private suspend fun processPurchase(purchase: Purchase): Unit = withContext(Dispatchers.IO) {
        if (purchase.products.size > 1) {
            NotificareLogger.warning("Detected a purchase with multiple products. Notificare only supports single-product purchases.")
        }

        val identifier = purchase.products.firstOrNull() ?: run {
            NotificareLogger.warning("Unable to process a purchase with no products.")
            return@withContext
        }

        val product = products[identifier]
        val productDetails = productDetails[identifier]
        if (product == null || productDetails == null) {
            throw RuntimeException("Unable to process a purchase when the product is not cached.")
        }

        val oneTimePurchaseOfferDetails = productDetails.oneTimePurchaseOfferDetails ?: run {
            throw RuntimeException("Only one time purchases are supported.")
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
            } else {
                NotificareLogger.warning("Something went wrong while consuming the purchase.")
                NotificareLogger.debug("$result")

                throw BillingException(result.billingResult.responseCode, result.billingResult.debugMessage)
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
            } else {
                NotificareLogger.warning("Something went wrong while acknowledging the purchase.")
                NotificareLogger.debug("$result")

                throw BillingException(result.responseCode, result.debugMessage)
            }

            return@withContext
        }

        NotificareLogger.debug("No action necessary for purchase order '${purchase.orderId}'.")
    }
}
