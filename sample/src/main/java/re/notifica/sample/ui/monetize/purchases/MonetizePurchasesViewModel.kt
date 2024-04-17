package re.notifica.sample.ui.monetize.purchases

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.monetize.ktx.monetize
import re.notifica.monetize.models.NotificarePurchase

class MonetizePurchasesViewModel : ViewModel() {
    private val _purchasesList = MutableLiveData<List<NotificarePurchase>>()
    val purchasesList: LiveData<List<NotificarePurchase>> = _purchasesList

    init {
        _purchasesList.postValue(Notificare.monetize().purchases)

        viewModelScope.launch {
            Notificare.monetize().observablePurchases
                .asFlow()
                .collect { purchases ->
                    _purchasesList.postValue(purchases)
                }
        }
    }
}
