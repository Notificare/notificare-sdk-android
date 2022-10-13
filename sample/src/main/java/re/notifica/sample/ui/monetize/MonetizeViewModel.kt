package re.notifica.sample.ui.monetize

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.monetize.ktx.monetize
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchase

class MonetizeViewModel : ViewModel() {
    private val _mediatorPurchasedProducts = MediatorLiveData<Pair<List<NotificareProduct>, List<NotificarePurchase>>>()
        .apply {
            addSource(Notificare.monetize().observableProducts) { value = Pair(it, value?.second ?: listOf()) }
            addSource(Notificare.monetize().observablePurchases) { value = Pair(value?.first ?: listOf(), it) }
        }

    private val _purchasesList = MutableLiveData<List<NotificarePurchase>>()
    val purchasesList: LiveData<List<NotificarePurchase>> = _purchasesList

    private val _productsList = MutableLiveData<List<ProductItem>>()
    val productsList: LiveData<List<ProductItem>> = _productsList

    init {
        viewModelScope.launch {
            _mediatorPurchasedProducts
                .asFlow()
                .collect { combined ->
                    val products = combined.first
                    val purchases = combined.second

                    val data: List<ProductItem> = products.map { product ->
                        ProductItem(
                            product = product,
                            purchases = purchases.filter { it.productIdentifier == product.identifier }
                        )
                    }
                    _purchasesList.postValue(purchases)
                    _productsList.postValue(data)
                }
        }
    }

    data class ProductItem(
        val product: NotificareProduct,
        val purchases: List<NotificarePurchase>,
    )
}
