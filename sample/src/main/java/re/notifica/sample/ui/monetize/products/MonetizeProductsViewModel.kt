package re.notifica.sample.ui.monetize.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.monetize.ktx.monetize
import re.notifica.monetize.models.NotificareProduct

class MonetizeProductsViewModel : ViewModel() {
    private val _productsList = MutableLiveData<List<NotificareProduct>>()
    val productsList: LiveData<List<NotificareProduct>> = _productsList

    init {
        _productsList.postValue(Notificare.monetize().products)

        viewModelScope.launch {
            Notificare.monetize().observableProducts
                .asFlow()
                .collect { products ->
                    _productsList.postValue(products)
                }
        }
    }
}
