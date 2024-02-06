package re.notifica.sample.ui.assets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.assets.ktx.assets
import re.notifica.assets.models.NotificareAsset
import re.notifica.sample.core.BaseViewModel
import timber.log.Timber

class AssetsViewModel : BaseViewModel() {
    private val _assets = MutableLiveData<List<NotificareAsset>>()
    val assets: LiveData<List<NotificareAsset>> = _assets

    fun fetchAssets(group: String) {
        viewModelScope.launch {
            try {
                val assets = Notificare.assets().fetch(group)
                _assets.postValue(assets)

                Timber.i("Fetch assets successfully")
                showSnackBar("Fetch assets successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch assets")
                showSnackBar("Failed to fetch assets: ${e.message}")
            }
        }
    }
}
